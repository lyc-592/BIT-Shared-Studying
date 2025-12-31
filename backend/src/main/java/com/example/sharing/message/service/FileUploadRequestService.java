package com.example.sharing.message.service;

import com.example.sharing.core.dto.ApiResponse;
import com.example.sharing.message.dto.FileUploadRequestDTO;
import com.example.sharing.message.entity.FileUploadRequest;
import com.example.sharing.message.entity.Message;
import com.example.sharing.message.enums.FileUploadStatus;
import com.example.sharing.message.enums.MessageType;
import com.example.sharing.message.repository.FileUploadRequestRepository;
import com.example.sharing.message.repository.MessageRepository;
import com.example.sharing.core.entity.User;
import com.example.sharing.core.repository.UserRepository;
import com.example.sharing.role.UserCoursePermission;
import com.example.sharing.role.UserCoursePermissionRepository;
import com.example.sharing.core.entity.Course;
import com.example.sharing.core.repository.CourseRepository;
import org.springframework.core.io.Resource;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadRequestService {

    private final FileUploadRequestRepository fileUploadRequestRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserCoursePermissionRepository userCoursePermissionRepository;
    private final CourseRepository courseRepository; // 若已有就不用加
    private final AiReviewService aiReviewService;

    // 待确认文件夹路径（根据你实际情况改成配置）
    private final String pendingBaseDir = "/root/unsure";
    private final String approvedBaseDir = "/root/unsure";

//    private final String pendingBaseDir = "D:/myfiles2";
//    private final String approvedBaseDir = "D:/myfiles2";

    public FileUploadRequestService(FileUploadRequestRepository fileUploadRequestRepository,
                                    MessageRepository messageRepository,
                                    UserRepository userRepository,
                                    UserCoursePermissionRepository userCoursePermissionRepository,
                                    CourseRepository courseRepository,
                                    AiReviewService aiReviewService) {
        this.fileUploadRequestRepository = fileUploadRequestRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.userCoursePermissionRepository = userCoursePermissionRepository;
        this.courseRepository = courseRepository;
        this.aiReviewService = aiReviewService;
    }

    /**
     * 普通用户提交“文件上传申请”
     * 这里只给出一个最小可用版本：
     * - 把文件存到 pending 目录
     * - 写一条 FileUploadRequest
     * - 暂时把所有 role>=2 的用户都当作审核人发消息（你之后可以改成按课程权限筛选）
     */
    @Transactional
    public ApiResponse<Void> submitUploadRequest(Long requesterId,
                                                 Long courseNo,
                                                 MultipartFile file,
                                                 String remark,
                                                 String targetAbsolutePath) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("申请用户不存在"));

        if (requester.getRole() == null || requester.getRole() != 1) {
            return ApiResponse.fail("只有普通用户可以发起上传申请");
        }
        if (courseNo == null) {
            return ApiResponse.fail("必须指定课程ID");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("必须上传文件");
        }

        // 保存到 pending 目录
        String pendingPath;
        String originalFilename = file.getOriginalFilename();
        try {
            pendingPath = savePendingFile(file);
        } catch (IOException e) {
            return ApiResponse.fail("文件保存失败，请稍后重试");
        }

        // 创建申请记录
        FileUploadRequest req = new FileUploadRequest();
        req.setRequester(requester);
        req.setCourseNo(courseNo);
        req.setOriginalFilename(originalFilename);
        req.setPendingFilePath(pendingPath);
        req.setStatus(FileUploadStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        req.setTargetAbsolutePath(targetAbsolutePath);
        fileUploadRequestRepository.save(req);

        // ===== 调用 AI 审查（一般是异步）=====
        try {
            aiReviewService.runAiReviewForRequest(req.getId());
        } catch (Exception e) {
            e.printStackTrace();
            // 不中断正常上传流程，AI 失败就当没填建议
        }

// ========= 只给“有该课程权限的用户”发消息 =========

// 1. 先根据 courseNo 拿到 Course 实体（course_no 就是主键）
        Course course = courseRepository.findById(courseNo)
                .orElse(null);
        if (course == null) {
            return ApiResponse.fail("课程不存在：" + courseNo);
        }

// 2. 从 user_course_permission 中找出“对这门课有权限的用户”
        List<UserCoursePermission> allPerms = userCoursePermissionRepository.findAll();

        List<User> reviewers = allPerms.stream()
                .filter(p -> p.getCourse() != null
                        && p.getCourse().getCourseNo().equals(course.getCourseNo()))
                .map(UserCoursePermission::getUser)
                .distinct()
                .toList();

        if (reviewers.isEmpty()) {
            return ApiResponse.fail("该课程暂无配置管理员权限，无法提交审核");
        }

// 3. 给这些用户逐个发 FILE_UPLOAD_REQUEST 消息
        for (User reviewer : reviewers) {
            Message msg = new Message();
            msg.setSender(requester);
            msg.setReceiver(reviewer);
            msg.setType(MessageType.FILE_UPLOAD_REQUEST);
            msg.setTitle("文件上传申请：用户 " + requester.getUsername()
                    + " 为课程 " + courseNo + " 上传文件 " + originalFilename);

            StringBuilder content = new StringBuilder();
            content.append("申请人：").append(requester.getUsername())
                    .append(" (ID=").append(requester.getId()).append(")\n")
                    .append("课程ID：").append(courseNo).append("\n")
                    .append("文件名：").append(originalFilename).append("\n");
            if (remark != null && !remark.isBlank()) {
                content.append("申请说明：").append(remark).append("\n");
            }

            msg.setContent(content.toString());
            msg.setFileUploadRequestId(req.getId());
            msg.setIsRead(false);
            msg.setCreatedAt(LocalDateTime.now());
            messageRepository.save(msg);
        }

        return ApiResponse.success("上传申请已提交，等待管理员审核", null);
    }

    private String savePendingFile(MultipartFile file) throws IOException {
        File dir = new File(pendingBaseDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建待确认目录");
        }
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        File dest = new File(dir, filename);
        file.transferTo(dest);
        return dest.getAbsolutePath();
    }

    private String moveToTargetAbsolutePath(String pendingPath,
                                            String targetAbsolutePath) throws Exception {
        Path source = Path.of(pendingPath);
        Path target = Path.of(targetAbsolutePath);

        // 确保目标目录存在
        Path parentDir = target.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // 移动（剪切）文件到目标路径
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        // 返回最终绝对路径
        return target.toAbsolutePath().toString();
    }

    private String moveToTargetDirectory(String pendingPath,
                                         String targetDirectory,
                                         String originalFilename) throws Exception {
        Path source = Path.of(pendingPath);
        Path targetDir = Path.of(targetDirectory);

        // 确保目标目录存在
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // 目标文件 = 目标目录 + 原始文件名
        Path targetFile = targetDir.resolve(originalFilename);

        // 剪切文件
        Files.move(source, targetFile, StandardCopyOption.REPLACE_EXISTING);

        // 返回最终文件绝对路径
        return targetFile.toAbsolutePath().toString();
    }

    @Transactional
    public ApiResponse<Void> approve(Long messageId, Long reviewerId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (msg.getType() != MessageType.FILE_UPLOAD_REQUEST) {
            return ApiResponse.fail("该消息不是文件上传申请请求");
        }

        if (!msg.getReceiver().getId().equals(reviewerId)) {
            return ApiResponse.fail("无权处理该申请");
        }

        Long reqId = msg.getFileUploadRequestId();
        if (reqId == null) {
            return ApiResponse.fail("消息未关联任何文件上传申请");
        }

        FileUploadRequest req = fileUploadRequestRepository.findById(reqId)
                .orElseThrow(() -> new RuntimeException("文件上传申请不存在"));

        if (req.getStatus() != FileUploadStatus.PENDING) {
            return ApiResponse.fail("该申请已被其他管理员处理");
        }

        // 移动文件：pending -> approved
        String pendingPath = req.getPendingFilePath();
        String targetDirectory = req.getTargetAbsolutePath(); // 现在当成目录
        String originalFilename = req.getOriginalFilename();

        if (pendingPath == null || pendingPath.isBlank()) {
            return ApiResponse.fail("待处理文件路径为空");
        }
        if (targetDirectory == null || targetDirectory.isBlank()) {
            return ApiResponse.fail("目标目录路径为空，无法转移");
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            return ApiResponse.fail("原始文件名为空，无法确定最终文件名");
        }

        try {
            String finalPath = moveToTargetDirectory(pendingPath, targetDirectory, originalFilename);
            req.setFinalFilePath(finalPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail("移动文件失败，请稍后重试");
        }

        // 更新状态
        req.setStatus(FileUploadStatus.APPROVED);
        req.setProcessedBy(msg.getReceiver());   // 或存 reviewerId
        req.setProcessedAt(LocalDateTime.now());
        fileUploadRequestRepository.save(req);

        // 给申请人发结果消息
        User requester = req.getRequester();
        Message resultMsg = new Message();
        resultMsg.setSender(msg.getReceiver());
        resultMsg.setReceiver(requester);
        resultMsg.setType(MessageType.FILE_UPLOAD_RESULT);
        resultMsg.setTitle("文件上传申请已通过");
        resultMsg.setContent("您为课程 " + req.getCourseNo() + " 上传的文件「"
                + req.getOriginalFilename() + "」已通过审核。");
        resultMsg.setIsRead(false);
        resultMsg.setCreatedAt(LocalDateTime.now());
        resultMsg.setFileUploadRequestId(req.getId());
        messageRepository.save(resultMsg);

        return ApiResponse.success("文件上传申请已通过", null);
    }

    /**
     * 审批拒绝：删除 pending 文件，更新状态，发结果消息
     */
    @Transactional
    public ApiResponse<Void> reject(Long messageId, Long reviewerId, String reason) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (msg.getType() != MessageType.FILE_UPLOAD_REQUEST) {
            return ApiResponse.fail("该消息不是文件上传申请请求");
        }

        if (!msg.getReceiver().getId().equals(reviewerId)) {
            return ApiResponse.fail("无权处理该申请");
        }

        Long reqId = msg.getFileUploadRequestId();
        if (reqId == null) {
            return ApiResponse.fail("消息未关联任何文件上传申请");
        }

        FileUploadRequest req = fileUploadRequestRepository.findById(reqId)
                .orElseThrow(() -> new RuntimeException("文件上传申请不存在"));

        if (req.getStatus() != FileUploadStatus.PENDING) {
            return ApiResponse.fail("该申请已被其他管理员处理");
        }

        // 删除 pending 文件（即使删失败，也不能影响业务主流程）
        String pendingPath = req.getPendingFilePath();
        if (pendingPath != null) {
            try {
                Files.deleteIfExists(Path.of(pendingPath));
            } catch (Exception e) {
                e.printStackTrace();
                // 不抛给前端，只记个日志
            }
        }

        req.setStatus(FileUploadStatus.REJECTED);
        req.setProcessedBy(msg.getReceiver());
        req.setProcessedAt(LocalDateTime.now());
        req.setRejectReason(reason);
        fileUploadRequestRepository.save(req);

        // 给申请人发结果消息
        User requester = req.getRequester();
        Message resultMsg = new Message();
        resultMsg.setSender(msg.getReceiver());
        resultMsg.setReceiver(requester);
        resultMsg.setType(MessageType.FILE_UPLOAD_RESULT);
        resultMsg.setTitle("文件上传申请被拒绝");
        StringBuilder content = new StringBuilder("您为课程 ")
                .append(req.getCourseNo()).append(" 上传的文件「")
                .append(req.getOriginalFilename()).append("」未通过审核。");
        if (reason != null && !reason.isBlank()) {
            content.append("\n原因：").append(reason);
        }
        resultMsg.setContent(content.toString());
        resultMsg.setIsRead(false);
        resultMsg.setCreatedAt(LocalDateTime.now());
        resultMsg.setFileUploadRequestId(req.getId());
        messageRepository.save(resultMsg);

        return ApiResponse.success("已拒绝该文件上传申请", null);
    }

    /**
     * 实际移动文件到 approved 目录
     */
    private String moveToApproved(String pendingPath, String originalFilename) throws Exception {
        Path source = Path.of(pendingPath);

        // 确保 approved 目录存在
        Path approvedDir = Path.of(approvedBaseDir);
        if (!Files.exists(approvedDir)) {
            Files.createDirectories(approvedDir);
        }

        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newName = java.util.UUID.randomUUID() + ext;
        Path target = approvedDir.resolve(newName);

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
    /**
     * 暂时只实现 DTO 转换，供 MessageService 使用
     */
    public FileUploadRequestDTO toDto(FileUploadRequest req) {
        if (req == null) {
            return null;
        }
        FileUploadRequestDTO dto = new FileUploadRequestDTO();
        dto.setId(req.getId());
        dto.setRequesterId(req.getRequester().getId());
        dto.setRequesterName(req.getRequester().getUsername());
        dto.setCourseNo(req.getCourseNo());
        dto.setOriginalFilename(req.getOriginalFilename());
        dto.setPendingFilePath(req.getPendingFilePath());
        dto.setFinalFilePath(req.getFinalFilePath());
        dto.setStatus(req.getStatus());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setRejectReason(req.getRejectReason());
        dto.setAiSuggestAction(req.getAiSuggestAction());
        dto.setAiSuggestReason(req.getAiSuggestReason());
        return dto;
    }

    private ResponseEntity<Resource> buildFileResponse(Path filePath,
                                                       boolean download) throws Exception {
        // 1. 检查文件是否存在且为普通文件
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new AccessDeniedException("指定路径不是文件: " + filePath);
        }

        // 2. 加载 Resource
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new AccessDeniedException("无法读取文件: " + filePath);
        }

        // 3. 文件名
        String filename = filePath.getFileName().toString();

        // 4. 内容类型（可以直接复用你 FileStorageService 里的 getMimeType 逻辑）
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (Exception e) {
            contentType = "application/octet-stream";
        }

        // 5. 编码文件名
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        // 6. 构建响应
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff");

        if (download) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0");
        } else {
            builder.header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);
        }

        return builder.body(resource);
    }

    // 预览待审核文件
    public ResponseEntity<Resource> previewPendingFile(Long requestId) {
        FileUploadRequest req = fileUploadRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("文件上传申请不存在"));

        String pendingPath = req.getPendingFilePath();
        if (pendingPath == null || pendingPath.isBlank()) {
            throw new RuntimeException("该申请没有待审核文件路径");
        }

        try {
            Path filePath = Paths.get(pendingPath);
            return buildFileResponse(filePath, false);   // false = 预览
        } catch (Exception e) {
            throw new RuntimeException("预览文件失败: " + e.getMessage(), e);
        }
    }

    // 下载待审核文件
    public ResponseEntity<Resource> downloadPendingFile(Long requestId) {
        FileUploadRequest req = fileUploadRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("文件上传申请不存在"));

        String pendingPath = req.getPendingFilePath();
        if (pendingPath == null || pendingPath.isBlank()) {
            throw new RuntimeException("该申请没有待审核文件路径");
        }

        try {
            Path filePath = Paths.get(pendingPath);
            return buildFileResponse(filePath, true);    // true = 下载
        } catch (Exception e) {
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
    // 审批通过/拒绝的逻辑我们可以下一步再详细写（移动/删除文件）
}