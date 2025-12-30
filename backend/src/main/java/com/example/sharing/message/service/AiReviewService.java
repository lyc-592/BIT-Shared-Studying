package com.example.sharing.message.service;

import com.example.sharing.core.dto.FileNode;
import com.example.sharing.message.entity.FileUploadRequest;
import com.example.sharing.message.repository.FileUploadRequestRepository;
import com.example.sharing.core.repository.CourseRepository;
import com.example.sharing.core.service.CourseFileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;



import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;



import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 针对 FileUploadRequest 做 AI 审查：
 * 1. 根据课程号获取课程目录树
 * 2. 先用“文件名相似度”筛候选文件
 * 3. 调用 DeepSeek 选出最相似的候选
 * 4. 按文件类型解析文本，再调用 DeepSeek 比较内容，生成不超过100字的建议
 * 5. 任何一步失败都兜底为 manual_review，不影响主流程
 */
@Service
public class AiReviewService {

    @Value("${file.storage.base-path}")
    private String COURSE_ROOT;
    private final CourseFileService courseFileService;
    private final CourseRepository courseRepository;
    private final FileUploadRequestRepository fileUploadRequestRepository;

    // 你自己的 DeepSeek HTTP 地址 + key，注意改成配置
    private final RestTemplate restTemplate = new RestTemplate();
    private final String deepSeekApiUrl = "https://api.deepseek.com/chat/completions";
    private final String deepSeekApiKey = "sk-d0c10cec220941418e77a4b108914124"; // TODO: 改为从配置读取
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AiReviewService(CourseFileService courseFileService,
                           CourseRepository courseRepository,
                           FileUploadRequestRepository fileUploadRequestRepository) {
        this.courseFileService = courseFileService;
        this.courseRepository = courseRepository;
        this.fileUploadRequestRepository = fileUploadRequestRepository;
    }

    /**
     * 对单条 FileUploadRequest 执行完整 AI 审查流程。
     * 本方法内部自己做兜底，不抛给外层。
     */
    @Transactional
    public void runAiReviewForRequest(Long requestId) {
        FileUploadRequest req = fileUploadRequestRepository.findById(requestId)
                .orElse(null);

        if (req == null) {
            return; // 记录不存在就直接返回
        }

        Long courseNo = req.getCourseNo();
        String newFilePath = req.getPendingFilePath();
        String newFilename = req.getOriginalFilename();
        String targetAbsolutePath = req.getTargetAbsolutePath();

        if (courseNo == null || newFilePath == null || newFilename == null) {
            saveAiSuggestion(req,
                    "manual_review",
                    "AI审查失败：缺少课程号或文件路径，请管理员人工审核。");
            return;
        }

        // 1. 获取课程目录树
        List<FileNode> courseFileTree;
        try {
            // 你原来的接口是 String courseId
            courseFileTree = courseFileService.getCourseFileTree(String.valueOf(courseNo));
        } catch (Exception e) {
            saveAiSuggestion(req,
                    "manual_review",
                    "AI审查失败：无法获取课程目录结构，请管理员人工审核。");
            return;
        }

        Path courseDir = extractCourseRootFromTarget(targetAbsolutePath);


        // 2. 基于文件名相似度筛候选
        List<CandidateFile> candidates =
                findSimilarNameCandidates(newFilename, courseFileTree, courseDir);
        if (candidates.isEmpty()) {
            saveAiSuggestion(req,
                    "none",
                    "AI未在该课程目录下发现与「" + newFilename +
                            "」明显相似的文件名，可正常进入人工审核流程。");
            return;
        }

        // 3. 让 DeepSeek 在候选中选最相似文件（基于名称）
        DeepSeekNameMatchResult nameMatchResult;
        try {
            nameMatchResult = callDeepSeekForBestNameMatch(newFilename, candidates, courseNo);
        } catch (Exception e) {
            // AI 名称比对失败，退回基于算法的人工审核提示
            CandidateFile top = candidates.get(0);
            saveAiSuggestion(req,
                    "manual_review",
                    "AI名称比对失败。系统检测到「" + newFilename +
                            "」可能与「" + top.getDisplayName() + "」重复，请管理员人工核对。");
            return;
        }

        if (nameMatchResult == null || !nameMatchResult.isHasSimilar()) {
            saveAiSuggestion(req,
                    "none",
                    "AI未在课程目录下发现与「" + newFilename +
                            "」高度相似的文件，请管理员正常审核。");
            return;
        }

        Integer idx = nameMatchResult.getBestMatchIndex();
        if (idx == null || idx < 0 || idx >= candidates.size()) {
            CandidateFile top = candidates.get(0);
            saveAiSuggestion(req,
                    "manual_review",
                    "AI返回的相似文件索引无效。系统检测到「" + newFilename +
                            "」可能与「" + top.getDisplayName() +
                            "」重复，请管理员人工核对。");
            return;
        }

        CandidateFile best = candidates.get(idx);
        String ext = getFileExtension(best.getPath()).toLowerCase(Locale.ROOT);

        // 4. 根据类型决定是否做内容比对
        if (isTextLike(ext)) {
            handleTextLike(req, newFilename, newFilePath, best);
            return;
        }

        if (isPptLike(ext)) {
            handlePptLike(req, newFilename, newFilePath, best);
            return;
        }

        // 其它类型（zip/rar/未知）：只给人工审核建议
        String oldRelPath = best.getRelativeToCourse();

        saveAiSuggestion(req,
                "manual_review",
                "文件类型为 " + ext +
                        "。系统检测到「" + newFilename +
                        "」可能与「" + best.getDisplayName() +
                        "」重复（基于文件名相似），请管理员人工核对内容。"
                        + "旧文件所在的路径为：" + oldRelPath);

    }

    /* ===================== 名称相似 & 候选 ===================== */

    public static class CandidateFile {
        private String path;           // 课程根目录下的相对路径
        private String absolutePath;   // 真实绝对路径
        private String displayName;    // 展示名
        private double nameScore;      // 文件名相似度分数
        private String relativeToCourse;

        // getters/setters

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getAbsolutePath() { return absolutePath; }
        public void setAbsolutePath(String absolutePath) { this.absolutePath = absolutePath; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public double getNameScore() { return nameScore; }
        public void setNameScore(double nameScore) { this.nameScore = nameScore; }

        public String getRelativeToCourse() {
            return relativeToCourse;
        }

        public void setRelativeToCourse(String relativeToCourse) {
            this.relativeToCourse = relativeToCourse;
        }
    }

    /**
     * 从课程目录树中找到与 newFilename 名称相似的候选文件。
     * 这里使用一个简单策略：
     * - 标准化文件名
     * - 用 Jaccard + 编辑距离的组合打分
     * - 分数 >= 0.5 的认为是候选，按分数倒序排序
     */
    private List<CandidateFile> findSimilarNameCandidates(String newFilename,
                                                          List<FileNode> tree,
                                                          Path courseDir) {
        String normNew = normalizeFilename(newFilename);

        List<CandidateFile> all = new ArrayList<>();
        collectCandidateFiles(tree, "", all, normNew, courseDir);

        // 过滤阈值，避免太乱
        double threshold = 0.5;
        return all.stream()
                .filter(c -> c.getNameScore() >= threshold)
                .sorted(Comparator.comparingDouble(CandidateFile::getNameScore).reversed())
                .limit(20) // 最多取前20个，避免给 AI 太多噪音
                .collect(Collectors.toList());
    }

    private void collectCandidateFiles(List<FileNode> nodes,
                                       String parentPath,
                                       List<CandidateFile> out,
                                       String normNew,
                                       Path courseDir) {
        if (nodes == null) return;

        for (FileNode node : nodes) {
            String relativePath = node.getPath(); // 课件/2023级/15-肖文卓-第2章.pdf 或类似

            String name = node.getName();

            if ("directory".equalsIgnoreCase(node.getType())) {
                collectCandidateFiles(node.getChildren(), relativePath, out, normNew, courseDir);
                continue;
            }
            if (!"file".equalsIgnoreCase(node.getType())) {
                continue;
            }

            String normOld = normalizeFilename(name);
            double score = calcNameSimilarity(normNew, normOld);

            // 统一用 / 作为内部相对路径分隔符
            String relToCourse = relativePath.replace('\\', '/');

            CandidateFile cf = new CandidateFile();
            cf.setPath(relToCourse);           // 用于计算绝对路径
            cf.setRelativeToCourse(relToCourse); // 也统一成 /

            cf.setDisplayName(name);

            // 这里用 Path，自动按系统分隔符拼接
            String absPath = courseDir.resolve(relToCourse)
                    .toAbsolutePath()
                    .normalize()
                    .toString();
            cf.setAbsolutePath(absPath);
            cf.setNameScore(score);

            out.add(cf);
        }
    }

    /** 标准化文件名：去扩展名、去空格/符号、转小写 */
    private String normalizeFilename(String name) {
        if (name == null) return "";
        // 去扩展名
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        // 去空格和常见符号
        name = name.replaceAll("[\\s\\-_.，,（）()【】\\[\\]]", "");
        return name.toLowerCase(Locale.ROOT);
    }

    /** 综合 Jaccard + 编辑距离 的简单相似度 */
    private double calcNameSimilarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        // 切成字符集合做 Jaccard
        Set<String> sa = Arrays.stream(a.split("")).collect(Collectors.toSet());
        Set<String> sb = Arrays.stream(b.split("")).collect(Collectors.toSet());
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        double jaccard = union.isEmpty() ? 0.0 : (double) inter.size() / union.size();

        // 简单 Levenshtein 编辑距离
        int dist = levenshtein(a, b);
        double edScore = 1.0 - (double) dist / Math.max(a.length(), b.length());

        // 综合
        return 0.5 * jaccard + 0.5 * edScore;
    }

    private int levenshtein(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[n][m];
    }

    /* ===================== DeepSeek 名称比对 ===================== */

    public static class DeepSeekNameMatchResult {
        private boolean hasSimilar;
        private Integer bestMatchIndex;
        private String bestMatchReason;

        public boolean isHasSimilar() { return hasSimilar; }
        public void setHasSimilar(boolean hasSimilar) { this.hasSimilar = hasSimilar; }

        public Integer getBestMatchIndex() { return bestMatchIndex; }
        public void setBestMatchIndex(Integer bestMatchIndex) { this.bestMatchIndex = bestMatchIndex; }

        public String getBestMatchReason() { return bestMatchReason; }
        public void setBestMatchReason(String bestMatchReason) { this.bestMatchReason = bestMatchReason; }
    }

    private DeepSeekNameMatchResult callDeepSeekForBestNameMatch(
            String newFilename,
            List<CandidateFile> candidates,
            Long courseNo
    ) {

        // 组装给 DeepSeek 的 prompt，要求严格 JSON 输出
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("你是一个教学资料重复检测助手。")
                .append("现在有一个新上传的文件和同一课程下的若干已有文件，请在这些候选中选出”最相似的一个“，")
                .append("只考虑文件名相似度，不看内容。")
                .append("请严格按照以下JSON格式输出：")
                .append("\n{\n")
                .append("  \"has_similar\": true/false,\n")
                .append("  \"best_match_index\": 整数索引(当has_similar为true时必须给出，从0开始),\n")
                .append("  \"best_match_reason\": \"不超过100字的中文说明\"\n")
                .append("}\n")
                .append("不要输出任何多余文本。\n\n");

        userPrompt.append("课程号: ").append(courseNo).append("\n");
        userPrompt.append("新文件名: ").append(newFilename).append("\n\n");
        userPrompt.append("候选文件列表(索引从0开始):\n");
        for (int i = 0; i < candidates.size(); i++) {
            CandidateFile c = candidates.get(i);
            userPrompt.append(i)
                    .append(". 名称: ").append(c.getDisplayName())
                    .append("  相对路径: ").append(c.getPath())
                    .append("  预估相似度: ").append(String.format(Locale.ROOT, "%.2f", c.getNameScore()))
                    .append("\n");
        }

        String requestBody = buildDeepSeekChatRequest(userPrompt.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekApiKey);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        String resp = restTemplate.postForObject(deepSeekApiUrl, entity, String.class);
        if (resp == null) {
            throw new RuntimeException("DeepSeek 响应为空");
        }

        // 这里只做非常简单的 JSON 解析，你可以换成 Jackson
        // 期望 resp 里有 content 字段，是模型回复的 JSON 字符串
        String content = SimpleJsonUtil.extractFirstChoiceContent(resp);
        if (content == null) {
            throw new RuntimeException("未找到模型回复内容");
        }

        Map<String, Object> json = SimpleJsonUtil.parseJsonToMap(content);
        DeepSeekNameMatchResult result = new DeepSeekNameMatchResult();
        Object hasSimilar = json.get("has_similar");
        Object bestIdx = json.get("best_match_index");
        Object reason = json.get("best_match_reason");

        result.setHasSimilar(hasSimilar instanceof Boolean && (Boolean) hasSimilar);
        if (bestIdx instanceof Number) {
            result.setBestMatchIndex(((Number) bestIdx).intValue());
        }
        if (reason instanceof String) {
            result.setBestMatchReason((String) reason);
        }
        return result;
    }

    /* ===================== DeepSeek 内容比对 ===================== */

    public static class DeepSeekContentReviewResult {
        private String action; // approve / reject / manual_review
        private String reason; // 不超过100字

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    private DeepSeekContentReviewResult callDeepSeekForContentSimilarity(
            String newFilename,
            String newText,
            CandidateFile oldFile,
            String oldText,
            Long courseNo
    ) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("你是一个严格的教学资源重复检测助手。")
                .append("下面提供的是同一门课程中的两个文件的文本节选。")
                .append("请判断新文件是否与旧文件内容高度重复或本质相同，")
                .append("并给出一个不超过100字的中文建议。")
                .append("只能从以下三种行动中选择其一：\"approve\"(通过)、\"reject\"(拒绝上传，认为高度重复或不合适)、")
                .append("\"manual_review\"(需要人工进一步判断)。")
                .append("请严格按照以下JSON格式输出：\n")
                .append("{\n")
                .append("  \"action\": \"approve\"/\"reject\"/\"manual_review\",\n")
                .append("  \"reason\": \"不超过100字的中文理由\"\n")
                .append("}\n")
                .append("不要输出任何多余的文本或注释。\n\n");

        userPrompt.append("课程号: ").append(courseNo).append("\n");
        userPrompt.append("新文件名: ").append(newFilename).append("\n");
        userPrompt.append("旧文件名: ").append(oldFile.getDisplayName())
                .append("  相对路径: ").append(oldFile.getPath()).append("\n\n");

        userPrompt.append("【新文件内容节选】:\n")
                .append(limitText(newText, 4000)).append("\n\n"); // 防止过长
        userPrompt.append("【旧文件内容节选】:\n")
                .append(limitText(oldText, 4000)).append("\n\n");

        String requestBody = buildDeepSeekChatRequest(userPrompt.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekApiKey);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        String resp = restTemplate.postForObject(deepSeekApiUrl, entity, String.class);
        if (resp == null) {
            throw new RuntimeException("DeepSeek 响应为空");
        }

        String content = SimpleJsonUtil.extractFirstChoiceContent(resp);
        if (content == null) {
            throw new RuntimeException("未找到模型回复内容");
        }

        Map<String, Object> json = SimpleJsonUtil.parseJsonToMap(content);
        DeepSeekContentReviewResult r = new DeepSeekContentReviewResult();
        Object action = json.get("action");
        Object reason = json.get("reason");
        if (action instanceof String) {
            r.setAction((String) action);
        }
        if (reason instanceof String) {
            r.setReason((String) reason);
        }
        return r;
    }

    private String buildDeepSeekChatRequest(String userPrompt) {
        // 非严格 JSON 构造，实际可以用 Jackson 构建
        return "{\n" +
                "  \"model\": \"deepseek-chat\", \n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"user\", \"content\": " + SimpleJsonUtil.quote(userPrompt) + "}\n" +
                "  ]\n" +
                "}";
    }

    /* ===================== 文本抽取与类型判断 ===================== */

    private boolean isTextLike(String ext) {
        return ".pdf".equals(ext) || ".docx".equals(ext) || ".txt".equals(ext);
    }

    private boolean isPptLike(String ext) {
        return ".pptx".equals(ext);
    }

    private String getFileExtension(String path) {
        if (path == null) return "";
        int dot = path.lastIndexOf('.');
        if (dot < 0) return "";
        return path.substring(dot).toLowerCase(Locale.ROOT); // 形如 ".pdf"
    }

    private boolean isNullOrTooShort(String text) {
        return text == null || text.trim().length() < 50;
    }

    private String trimTo100Chars(String s) {
        if (s == null) return null;
        // 这里按字符数截断即可
        if (s.length() <= 100) return s;
        return s.substring(0, 100);
    }

    private String limitText(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen);
    }

    /** pdf/doc/docx/txt 抽文本 （这里只给 pdf/txt 的简单实现，Word 你可后续补） */
    private String extractTextFromFile(String absolutePath, String ext) throws Exception {
        Path p = Paths.get(absolutePath);
        if (!Files.exists(p)) {
            throw new FileNotFoundException("文件不存在: " + absolutePath);
        }
        ext = ext.toLowerCase(Locale.ROOT);

        if (".txt".equals(ext)) {
            // 如果不是 UTF-8，可根据实际情况再加 GBK 等判断
            return Files.readString(p, StandardCharsets.UTF_8);
        }

        if (".pdf".equals(ext)) {
            try (InputStream is = Files.newInputStream(p);
                 PDDocument doc = PDDocument.load(is)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        }

        if (".doc".equals(ext) || ".docx".equals(ext)) {
            return extractTextFromWord(absolutePath);
        }

        // 其他类型暂不支持，返回空串让上层走兜底
        return "";
    }

    private String extractTextFromWord(String absolutePath) throws Exception {
        String ext = getFileExtension(absolutePath).toLowerCase(Locale.ROOT);
        Path p = Paths.get(absolutePath);


        if (".docx".equals(ext)) {
            try (InputStream is = Files.newInputStream(p);
                 XWPFDocument doc = new XWPFDocument(is);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        }
        return "";
    }

    /** PPT/PPTX 抽 slide 文本（这里只写个伪实现，你用 Apache POI 补充） */
    private String extractTextFromPpt(String absolutePath) throws Exception {
        String ext = getFileExtension(absolutePath).toLowerCase(Locale.ROOT);
        Path p = Paths.get(absolutePath);
        if (!Files.exists(p)) {
            throw new FileNotFoundException("PPT 文件不存在: " + absolutePath);
        }

        StringBuilder sb = new StringBuilder();

        if (".pptx".equals(ext)) {
            try (InputStream is = Files.newInputStream(p);
                 XMLSlideShow ppt = new XMLSlideShow(is)) {
                for (XSLFSlide slide : ppt.getSlides()) {
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape textShape) {
                            String text = textShape.getText();
                            if (text != null && !text.isBlank()) {
                                sb.append(text.trim()).append("\n");
                            }
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    private void handleTextLike(FileUploadRequest req,
                                String newFilename,
                                String newFilePath,
                                CandidateFile best) {
        String ext = getFileExtension(best.getPath());
        String oldRelPath = best.getRelativeToCourse(); // 课件\2023级\xxx.pdf
        try {
            String newText = extractTextFromFile(newFilePath, ext);
            String oldText = extractTextFromFile(best.getAbsolutePath(), ext);


            if (isNullOrTooShort(newText) || isNullOrTooShort(oldText)) {

                saveAiSuggestion(req,
                        "manual_review",
                        "AI无法从文件中提取足够的文本内容。系统检测到「" + newFilename +
                                "」可能与「" + best.getDisplayName() +
                                "」重复，请管理员人工核对。旧文件所在的路径为：" + oldRelPath);
                return;
            }

            DeepSeekContentReviewResult r = callDeepSeekForContentSimilarity(
                    newFilename, newText, best, oldText, req.getCourseNo());
            if (r == null || r.getAction() == null || r.getReason() == null) {
                saveAiSuggestion(req,
                        "manual_review",
                        "AI内容比对返回结果不完整。系统检测到「" + newFilename +
                                "」可能与「" + best.getDisplayName() +
                                "」重复，请管理员人工核对。旧文件所在的路径为：" + oldRelPath);
                return;
            }

            String finalReason = r.getReason() + " 旧文件所在的路径为：" + oldRelPath;
            saveAiSuggestion(req, r.getAction(), finalReason);
        } catch (Exception e) {
            e.printStackTrace();
            saveAiSuggestion(req,
                    "manual_review",
                    "AI内容比对失败。系统检测到「" + newFilename +
                            "」可能与「" + best.getDisplayName() +
                            "」重复，请管理员人工核对。旧文件所在的路径为：" + oldRelPath);
        }
    }

    private void handlePptLike(FileUploadRequest req,
                               String newFilename,
                               String newFilePath,
                               CandidateFile best) {
        String oldRelPath = best.getRelativeToCourse();
        try {
            String newText = extractTextFromPpt(newFilePath);
            String oldText = extractTextFromPpt(best.getAbsolutePath());

            if (isNullOrTooShort(newText) || isNullOrTooShort(oldText)) {
                saveAiSuggestion(req,
                        "manual_review",
                        "PPT中文本内容过少或为空。系统检测到「" + newFilename +
                                "」可能与「" + best.getDisplayName() +
                                "」重复，请管理员人工核对。旧文件所在的路径为：" + oldRelPath);
                return;
            }

            DeepSeekContentReviewResult r = callDeepSeekForContentSimilarity(
                    newFilename, newText, best, oldText, req.getCourseNo());
            if (r == null || r.getAction() == null || r.getReason() == null) {
                saveAiSuggestion(req,
                        "manual_review",
                        "AI内容比对返回结果不完整。系统检测到「" + newFilename +
                                "」可能与「" + best.getDisplayName() +
                                "」重复，请管理员人工核对。旧文件所在的路径为：" + oldRelPath);
                return;
            }

            String finalReason = r.getReason() + " 旧文件所在的路径为：" + oldRelPath;
            saveAiSuggestion(req, r.getAction(), finalReason);
        } catch (Exception e) {
            saveAiSuggestion(req,
                    "manual_review",
                    "AI无法解析PPT文本内容。系统检测到「" + newFilename +
                            "」可能与「" + best.getDisplayName() +
                            "」重复，请管理员人工核对。旧文件所在的路径为：" + oldRelPath);
        }
    }

    /* ===================== 写回数据库 ===================== */

    private void saveAiSuggestion(FileUploadRequest req, String action, String reason) {
        req.setAiSuggestAction(action);
        req.setAiSuggestReason(trimTo100Chars(reason));
        fileUploadRequestRepository.save(req);
    }

    /* ===================== 简单 JSON 工具（你可换成 Jackson） ===================== */

    /**
     * 这里只写一个极简工具，你可以替换成自己常用的 JSON 工具库（Jackson / Fastjson）
     */
    static class SimpleJsonUtil {

        /**
         * 从 DeepSeek 的 chat completion 响应 JSON 中，
         * 提取第一条 choice.message.content 的字符串。
         */
        public static String extractFirstChoiceContent(String respJson) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(respJson);
                JsonNode choices = root.get("choices");
                if (choices == null || !choices.isArray() || choices.size() == 0) {
                    return null;
                }
                JsonNode first = choices.get(0);
                if (first == null) return null;
                JsonNode message = first.get("message");
                if (message == null) return null;
                JsonNode content = message.get("content");
                if (content == null || content.isNull()) return null;
                return content.asText();
            } catch (Exception e) {
                return null;
            }
        }

        /** 把 JSON 字符串解析成 Map<String, Object>（结构简单时够用） */
        @SuppressWarnings("unchecked")
        public static Map<String, Object> parseJsonToMap(String json) {
            try {
                return OBJECT_MAPPER.readValue(json, Map.class);
            } catch (Exception e) {
                return Collections.emptyMap();
            }
        }

        /** 简单字符串转义，用于拼接 JSON 文本 */
        public static String quote(String s) {
            try {
                return OBJECT_MAPPER.writeValueAsString(s == null ? "" : s);
            } catch (Exception e) {
                return "\"\"";
            }
        }
    }

    private Path extractCourseRootFromTarget(String targetAbsolutePath) {
        if (targetAbsolutePath == null || targetAbsolutePath.isBlank()) {
            return null;
        }

        // 用 Path 处理，自动适配 Linux/Windows
        Path full = Paths.get(targetAbsolutePath).toAbsolutePath().normalize();

        // 从右往左找“包含 _ 的目录名”（课程目录）
        Path cur = full;
        Path courseDir = null;

        while (cur != null) {
            Path fileName = cur.getFileName();
            if (fileName != null) {
                String name = fileName.toString();
                if (name.contains("_")) {
                    courseDir = cur;
                    break;
                }
            }
            cur = cur.getParent();
        }

        // 一个带 _ 的目录都没找到，就退回整个路径（兜底）
        if (courseDir == null) {
            return full;
        }

        return courseDir;
    }
}