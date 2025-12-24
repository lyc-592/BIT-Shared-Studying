package com.example.sharing.message.service;

import com.example.sharing.dto.ApiResponse;
import com.example.sharing.message.dto.AdminApplyDTO;
import com.example.sharing.message.entity.AdminApply;
import com.example.sharing.message.entity.Message;
import com.example.sharing.message.enums.ApplyStatus;
import com.example.sharing.message.enums.MessageType;
import com.example.sharing.message.repository.AdminApplyRepository;
import com.example.sharing.message.repository.MessageRepository;
import com.example.sharing.role.PermissionService;
import com.example.sharing.role.GrantPermissionRequest;
import com.example.sharing.entity.User;
import com.example.sharing.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminApplyService {

    private final AdminApplyRepository adminApplyRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    // 申请资料存放目录（你可改为配置）
    private final String adminApplyBaseDir = "/root/Apply";

    public AdminApplyService(AdminApplyRepository adminApplyRepository,
                             MessageRepository messageRepository,
                             UserRepository userRepository,
                             PermissionService permissionService) {
        this.adminApplyRepository = adminApplyRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    /**
     * 普通用户提交成为某专业的二级管理员申请
     */
    @Transactional
    public ApiResponse<Void> submitApply(Long applicantId,
                                         Long majorNo,
                                         MultipartFile wordFile,
                                         String remark) {
        Optional<User> applicantOpt = userRepository.findById(applicantId);
        if (applicantOpt.isEmpty()) {
            return ApiResponse.fail("申请用户不存在");
        }
        User applicant = applicantOpt.get();

        // 只有普通用户才能发起申请（这里按你角色约定，role=1 是普通）
        if (applicant.getRole() == null || applicant.getRole() != 1) {
            return ApiResponse.fail("只有普通用户可以申请成为管理员");
        }

        if (majorNo == null) {
            return ApiResponse.fail("必须指定专业ID");
        }
        if (wordFile == null || wordFile.isEmpty()) {
            return ApiResponse.fail("必须上传申请资料Word文件");
        }

        // 检查是否已有待审核的同专业申请
        if (adminApplyRepository
                .findByApplicantAndMajorNoAndStatus(applicant, majorNo, ApplyStatus.PENDING)
                .isPresent()) {
            return ApiResponse.fail("你已提交过该专业的申请，正在审核中");
        }

        // 保存 Word 文件
        String wordPath;
        try {
            wordPath = saveWordFile(wordFile);
        } catch (IOException e) {
            return ApiResponse.fail("申请资料保存失败，请稍后重试");
        }

        // 创建申请记录
        AdminApply apply = new AdminApply();
        apply.setApplicant(applicant);
        apply.setMajorNo(majorNo);
        apply.setStatus(ApplyStatus.PENDING);
        apply.setWordFilePath(wordPath);
        apply.setCreatedAt(LocalDateTime.now());
        adminApplyRepository.save(apply);

        // 查找所有 role>=3 的管理员作为审核人
        List<User> reviewers = userRepository.findByRoleGreaterThanEqual(3);
        if (reviewers.isEmpty()) {
            return ApiResponse.fail("当前没有可用的审核管理员，请联系系统管理员");
        }

        // 给每位审核人发送消息
        for (User reviewer : reviewers) {
            Message msg = new Message();
            msg.setSender(applicant);
            msg.setReceiver(reviewer);
            msg.setType(MessageType.ADMIN_APPLY_REQUEST);
            msg.setTitle("管理员申请：用户 " + applicant.getUsername()
                    + " 申请成为专业 " + majorNo + " 的管理员");
            StringBuilder content = new StringBuilder();
            content.append("申请人：").append(applicant.getUsername())
                    .append(" (ID=").append(applicant.getId()).append(")\n")
                    .append("申请专业ID：").append(majorNo).append("\n");
            if (remark != null && !remark.isBlank()) {
                content.append("申请说明：").append(remark).append("\n");
            }
            msg.setContent(content.toString());
            msg.setAdminApplyId(apply.getId());
            msg.setIsRead(false);
            msg.setCreatedAt(LocalDateTime.now());
            messageRepository.save(msg);
        }

        return ApiResponse.success("申请已提交，等待管理员审核", null);
    }

    private String saveWordFile(MultipartFile file) throws IOException {
        File dir = new File(adminApplyBaseDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建申请资料目录");
        }
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;
        File dest = new File(dir, filename);
        file.transferTo(dest);
        // 这里返回绝对路径或相对路径，看你现有文件服务是怎么设计的
        return dest.getAbsolutePath();
    }

    /**
     * 审核管理员同意某条管理员申请
     * @param messageId  消息ID（从信箱点进来的那条）
     * @param reviewerId 当前审核人ID
     */
    @Transactional
    public ApiResponse<Void> approve(Long messageId, Long reviewerId) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isEmpty()) {
            return ApiResponse.fail("消息不存在");
        }
        Message msg = msgOpt.get();

        if (msg.getType() != MessageType.ADMIN_APPLY_REQUEST) {
            return ApiResponse.fail("该消息不是管理员申请");
        }
        if (!msg.getReceiver().getId().equals(reviewerId)) {
            return ApiResponse.fail("你无权处理这条消息");
        }

        Long applyId = msg.getAdminApplyId();
        if (applyId == null) {
            return ApiResponse.fail("该消息未关联申请记录");
        }

        AdminApply apply = adminApplyRepository.findById(applyId)
                .orElse(null);
        if (apply == null) {
            return ApiResponse.fail("申请记录不存在");
        }

        // 只要有一个人先处理，其它人不能再改：检查状态是否还是 PENDING
        if (apply.getStatus() != ApplyStatus.PENDING) {
            return ApiResponse.fail("该申请已被其他管理员处理");
        }

        // 修改状态为 APPROVED
        apply.setStatus(ApplyStatus.APPROVED);
        apply.setProcessedBy(msg.getReceiver());
        apply.setProcessedAt(LocalDateTime.now());
        adminApplyRepository.save(apply);

        // 调用已有授权逻辑：授予二级 + 专业ID
        GrantPermissionRequest grantReq = new GrantPermissionRequest();
        grantReq.setGrantorId(reviewerId);
        grantReq.setTargetUsername(apply.getApplicant().getUsername());
        grantReq.setTargetRole(2);  // 二级管理员
        grantReq.setMajorNo(apply.getMajorNo());

        String error = permissionService.grantPermission(grantReq);
        if (error != null) {
            // 授权失败则回滚事务
            throw new RuntimeException("授权失败：" + error);
        }

        // 给申请人发结果消息
        Message result = new Message();
        result.setSender(msg.getReceiver()); // 审核人
        result.setReceiver(apply.getApplicant());
        result.setType(MessageType.ADMIN_APPLY_RESULT);
        result.setTitle("你的管理员申请已通过");
        result.setContent("你已成为专业 " + apply.getMajorNo() + " 的管理员。");
        result.setAdminApplyId(apply.getId());
        result.setIsRead(false);
        result.setCreatedAt(LocalDateTime.now());
        messageRepository.save(result);

        // 把当前这条请求消息标记为已读（可选）
        msg.setIsRead(true);
        msg.setReadAt(LocalDateTime.now());
        messageRepository.save(msg);

        return ApiResponse.success("审批已通过", null);
    }

    /**
     * 审核管理员拒绝某条管理员申请
     */
    @Transactional
    public ApiResponse<Void> reject(Long messageId,
                                    Long reviewerId,
                                    String reason) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isEmpty()) {
            return ApiResponse.fail("消息不存在");
        }
        Message msg = msgOpt.get();

        if (msg.getType() != MessageType.ADMIN_APPLY_REQUEST) {
            return ApiResponse.fail("该消息不是管理员申请");
        }
        if (!msg.getReceiver().getId().equals(reviewerId)) {
            return ApiResponse.fail("你无权处理这条消息");
        }

        Long applyId = msg.getAdminApplyId();
        if (applyId == null) {
            return ApiResponse.fail("该消息未关联申请记录");
        }

        AdminApply apply = adminApplyRepository.findById(applyId)
                .orElse(null);
        if (apply == null) {
            return ApiResponse.fail("申请记录不存在");
        }

        // 只允许第一个处理
        if (apply.getStatus() != ApplyStatus.PENDING) {
            return ApiResponse.fail("该申请已被其他管理员处理");
        }

        apply.setStatus(ApplyStatus.REJECTED);
        apply.setProcessedBy(msg.getReceiver());
        apply.setProcessedAt(LocalDateTime.now());
        apply.setRejectReason(reason);
        adminApplyRepository.save(apply);

        // 给申请人发结果消息
        Message result = new Message();
        result.setSender(msg.getReceiver());
        result.setReceiver(apply.getApplicant());
        result.setType(MessageType.ADMIN_APPLY_RESULT);
        result.setTitle("你的管理员申请被拒绝");
        String content = "你的管理员申请已被拒绝。原因：" +
                (reason == null || reason.isBlank() ? "无" : reason);
        result.setContent(content);
        result.setAdminApplyId(apply.getId());
        result.setIsRead(false);
        result.setCreatedAt(LocalDateTime.now());
        messageRepository.save(result);

        msg.setIsRead(true);
        msg.setReadAt(LocalDateTime.now());
        messageRepository.save(msg);

        return ApiResponse.success("已拒绝该申请", null);
    }

    /**
     * 用于将实体转为 DTO（供 MessageService / Controller 复用）
     */
    public AdminApplyDTO toDto(AdminApply apply) {
        if (apply == null) return null;
        AdminApplyDTO dto = new AdminApplyDTO();
        dto.setId(apply.getId());
        dto.setApplicantId(apply.getApplicant().getId());
        dto.setApplicantName(apply.getApplicant().getUsername());
        dto.setMajorNo(apply.getMajorNo());
        dto.setStatus(apply.getStatus());
        dto.setWordFilePath(apply.getWordFilePath());
        dto.setCreatedAt(apply.getCreatedAt());
        dto.setRejectReason(apply.getRejectReason());
        return dto;
    }

    // ================== 简历预览 / 下载 ==================

    /**
     * 预览管理员申请的简历 Word
     */
    public ResponseEntity<Resource> previewWord(Long applyId) {
        AdminApply apply = adminApplyRepository.findById(applyId)
                .orElseThrow(() -> new RuntimeException("管理员申请不存在"));

        String path = apply.getWordFilePath();
        if (path == null || path.isBlank()) {
            throw new RuntimeException("该申请没有上传简历");
        }

        try {
            Path filePath = Paths.get(path);
            return buildWordFileResponse(filePath, false); // false = 预览
        } catch (Exception e) {
            throw new RuntimeException("预览简历失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载管理员申请的简历 Word
     */
    public ResponseEntity<Resource> downloadWord(Long applyId) {
        AdminApply apply = adminApplyRepository.findById(applyId)
                .orElseThrow(() -> new RuntimeException("管理员申请不存在"));

        String path = apply.getWordFilePath();
        if (path == null || path.isBlank()) {
            throw new RuntimeException("该申请没有上传简历");
        }

        try {
            Path filePath = Paths.get(path);
            return buildWordFileResponse(filePath, true); // true = 下载
        } catch (Exception e) {
            throw new RuntimeException("下载简历失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通用的 Word 文件响应构造（按绝对路径）
     */
    private ResponseEntity<Resource> buildWordFileResponse(Path filePath,
                                                           boolean download) throws Exception {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new AccessDeniedException("指定路径不是文件: " + filePath);
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new AccessDeniedException("无法读取文件: " + filePath);
        }

        String filename = filePath.getFileName().toString();

        // MIME 类型，优先 probe，空的话按扩展名兜底
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                String lower = filename.toLowerCase();
                if (lower.endsWith(".doc")) {
                    contentType = "application/msword";
                } else if (lower.endsWith(".docx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else {
                    contentType = "application/octet-stream";
                }
            }
        } catch (Exception e) {
            contentType = "application/octet-stream";
        }

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

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
}