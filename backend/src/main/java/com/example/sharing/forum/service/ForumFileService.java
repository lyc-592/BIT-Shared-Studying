package com.example.sharing.forum.service;

import com.example.sharing.forum.entity.Attachment;
import com.example.sharing.forum.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumFileService {

    private final AttachmentRepository attachmentRepository;

    @Value("${file.storage.attachment-path}")
    private String attachmentPath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // 允许的文件类型
    private static final String[] ALLOWED_FILE_TYPES = {
            "image/jpeg", "image/png", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/x-rar-compressed"
    };

    // 最大文件大小：50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 保存论坛附件（采用唯一文件名存储）
     */
    public List<Attachment> saveForumAttachments(List<MultipartFile> files, Long userId, Long forumNo, Long topicId, Long commentId) {
        List<Attachment> attachments = new ArrayList<>();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        Attachment attachment = saveSingleAttachment(file, userId, forumNo, topicId, commentId);
                        if (attachment != null) {
                            attachments.add(attachment);
                        }
                    } catch (Exception e) {
                        log.error("保存附件失败: {}", e.getMessage(), e);
                        // 继续处理其他文件
                    }
                }
            }
        }

        return attachments;
    }

    /**
     * 保存单个附件（使用唯一文件名）
     */
    private Attachment saveSingleAttachment(MultipartFile file, Long userId, Long forumNo, Long topicId, Long commentId) {
        try {
            // 1. 验证文件
            validateFile(file);

            // 2. 生成唯一的文件名（UUID）
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);

            // 3. 构建存储目录结构（按论坛存储，可选按日期分目录）
            String storageDir = buildStorageDirectory(forumNo, true); // true表示按日期分目录

            // 4. 构建完整的存储路径（使用Path确保跨平台兼容性）
            Path storageDirPath = Paths.get(attachmentPath, storageDir.split("/"));
            Path fullPath = storageDirPath.resolve(uniqueFilename);
            String relativePath = Paths.get(storageDir, uniqueFilename).toString();

            // 5. 创建目录（如果不存在）
            createDirectoryIfNotExists(storageDirPath);

            // 6. 保存文件
            saveFileToDisk(file, fullPath, uniqueFilename);

            // 7. 生成访问URL
            String accessUrl = generateAttachmentAccessUrl(forumNo, uniqueFilename);

            // 8. 创建Attachment实体
            Attachment attachment = new Attachment();
            attachment.setUserId(userId);
            attachment.setTopicId(topicId);
            attachment.setCommentId(commentId);
            attachment.setOriginalName(originalFilename);
            attachment.setStorageKey(relativePath);  // 存储相对路径
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStorageType("local");
            attachment.setAccessUrl(accessUrl);
            attachment.setCreatedAt(LocalDateTime.now());

            return attachmentRepository.save(attachment);

        } catch (Exception e) {
            log.error("保存附件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过 50MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new RuntimeException("无法识别文件类型");
        }

        boolean allowed = false;
        for (String allowedType : ALLOWED_FILE_TYPES) {
            if (allowedType.equals(contentType)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new RuntimeException("不支持的文件类型: " + contentType);
        }
    }

    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(Path dirPath) {
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("创建目录成功: {}", dirPath);
            }
        } catch (Exception e) {
            log.error("创建目录失败: {}", dirPath, e);
            throw new RuntimeException("无法创建附件存储目录: " + e.getMessage());
        }
    }

    /**
     * 保存文件到磁盘
     */
    private void saveFileToDisk(MultipartFile file, Path fullPath, String uniqueFilename) {
        try {
            // 清理文件名，防止路径注入
            String safeFilename = sanitizeFilename(uniqueFilename);
            Path targetFile = fullPath.normalize();

            // 保存文件（存在则覆盖）
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件保存成功: {}", fullPath);
        } catch (IOException e) {
            log.error("保存文件到磁盘失败: {}", fullPath, e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 清理文件名，防止路径注入
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }
        // 移除路径分隔符和特殊字符
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 构建存储目录结构
     * @param forumNo 论坛编号
     * @param useDateSubdir 是否使用日期子目录
     * @return 存储目录（相对路径，使用/分隔符，因为这是URL友好的格式）
     */
    private String buildStorageDirectory(Long forumNo, boolean useDateSubdir) {
        StringBuilder dirBuilder = new StringBuilder();

        // 1. 论坛文件夹
        dirBuilder.append("forum_").append(forumNo);

        // 2. 可选：日期子目录（便于管理）
        if (useDateSubdir) {
            String currentMonth = LocalDateTime.now().format(DATE_FORMATTER);
            dirBuilder.append("/").append(currentMonth);
        }

        return dirBuilder.toString();
    }

    /**
     * 生成唯一的文件名
     */
    private String generateUniqueFilename(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        if (extension != null && !extension.isEmpty()) {
            return uuid + "." + extension.toLowerCase();
        }

        // 如果没有扩展名，检查文件类型
        return uuid + ".dat"; // 默认扩展名
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }

        return null;
    }

    /**
     * 生成附件访问URL
     */
    private String generateAttachmentAccessUrl(Long forumNo, String filename) {
        // 生成下载URL，例如：/api/attachments/download/{forumNo}/{filename}
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/attachments/download/{forumNo}/{filename}")
                .buildAndExpand(forumNo, filename)
                .toUriString();
    }

    /**
     * 删除附件
     */
    public boolean deleteAttachment(Long attachmentId, Long userId) {
        try {
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("附件不存在"));

            // 权限检查
            if (!attachment.getUserId().equals(userId)) {
                throw new RuntimeException("无权删除此附件");
            }

            // 获取物理路径
            String fullPath = getAttachmentPhysicalPath(attachment);

            // 先删除数据库记录
            attachmentRepository.delete(attachment);

            // 然后删除物理文件（即使失败也不影响数据库）
            try {
                deleteFileFromDisk(fullPath);
            } catch (Exception e) {
                log.error("删除物理文件失败，但数据库记录已删除: {}", fullPath, e);
            }

            log.info("附件删除成功: attachmentId={}, userId={}", attachmentId, userId);
            return true;

        } catch (Exception e) {
            log.error("删除附件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从磁盘删除文件
     */
    private void deleteFileFromDisk(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("物理文件删除成功: {}", filePath);

                // 尝试删除空目录
                deleteEmptyParentDirectories(path.getParent());
            }
        } catch (Exception e) {
            log.error("删除物理文件失败: {}", filePath, e);
            // 继续删除数据库记录，不抛出异常
        }
    }

    /**
     * 删除空的父目录
     */
    private void deleteEmptyParentDirectories(Path directory) {
        try {
            Path basePath = Paths.get(attachmentPath).toAbsolutePath().normalize();
            Path currentDir = directory.toAbsolutePath().normalize();

            // 确保不会删除基础目录
            while (currentDir != null && Files.exists(currentDir) &&
                    !currentDir.equals(basePath) && currentDir.startsWith(basePath)) {

                // 检查目录是否为空
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(currentDir)) {
                    if (!dirStream.iterator().hasNext()) {
                        // 目录为空，删除它
                        Files.delete(currentDir);
                        log.info("删除空目录: {}", currentDir);
                        currentDir = currentDir.getParent();
                    } else {
                        // 目录不为空，停止删除
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略删除目录时的错误
        }
    }

    /**
     * 获取附件的物理路径
     */
    public String getAttachmentPhysicalPath(Attachment attachment) {
        String relativePath = attachment.getStorageKey();
        // 将存储路径中的/替换为当前系统的文件分隔符
        Path fullPath = Paths.get(attachmentPath, relativePath.split("/"));
        return fullPath.toString();
    }

    /**
     * 获取话题的所有附件
     */
    public List<Attachment> getAttachmentsByTopicId(Long topicId) {
        return attachmentRepository.findByTopicId(topicId);
    }

    /**
     * 通过论坛号和文件名查找附件
     */
    public Attachment findByForumNoAndFilename(Long forumNo, String filename) {
        try {
            log.info("查找附件 - forumNo: {}, filename: {}", forumNo, filename);

            if (forumNo == null || filename == null) {
                return null;
            }

            // 清理文件名，只获取最后的文件名部分（去除路径）
            String cleanFilename;
            if (filename.contains("/")) {
                String[] parts = filename.split("/");
                cleanFilename = parts[parts.length - 1];
                log.debug("清理文件名: {} -> {}", filename, cleanFilename);
            } else {
                cleanFilename = filename;
            }

            // 1. 首先尝试直接通过存储路径查找（完整路径匹配）
            String exactPathPattern = "forum_" + forumNo + "/" + cleanFilename;
            log.debug("尝试精确匹配: {}", exactPathPattern);

            Optional<Attachment> exactMatch = attachmentRepository.findByStorageKeyContaining(exactPathPattern);
            if (exactMatch.isPresent()) {
                Attachment attachment = exactMatch.get();
                log.info("精确匹配成功 - id: {}, storageKey: {}", attachment.getId(), attachment.getStorageKey());
                return attachment;
            }

            // 2. 如果精确匹配失败，查找属于该论坛的所有附件
            String forumDir = "forum_" + forumNo;
            log.debug("查找属于论坛 {} 的所有附件", forumDir);

            // 获取所有附件，然后过滤出属于该论坛的
            List<Attachment> allAttachments = attachmentRepository.findAll();
            List<Attachment> forumAttachments = allAttachments.stream()
                    .filter(a -> a.getStorageKey() != null && a.getStorageKey().startsWith(forumDir + "/"))
                    .collect(Collectors.toList());

            log.debug("找到属于论坛 {} 的 {} 个附件", forumNo, forumAttachments.size());

            // 3. 在这些附件中查找匹配文件名的附件
            for (Attachment attachment : forumAttachments) {
                String storageKey = attachment.getStorageKey();

                // 从存储路径中提取文件名（最后一部分）
                String[] keyParts = storageKey.split("/");
                String storedFilename = keyParts[keyParts.length - 1];

                // 匹配文件名
                if (storedFilename.equals(cleanFilename)) {
                    log.info("通过论坛内查找匹配到附件 - id: {}, storageKey: {}",
                            attachment.getId(), storageKey);
                    return attachment;
                }

                // 也匹配原始文件名（可能包含中文）
                if (attachment.getOriginalName() != null &&
                        attachment.getOriginalName().equals(cleanFilename)) {
                    log.info("通过原始文件名匹配到附件 - id: {}, originalName: {}",
                            attachment.getId(), attachment.getOriginalName());
                    return attachment;
                }
            }

            // 4. 最后尝试模糊查找（包含文件名即可）
            log.debug("尝试模糊查找包含文件名的附件: {}", cleanFilename);
            Optional<Attachment> fuzzyMatch = allAttachments.stream()
                    .filter(a -> a.getStorageKey() != null && a.getStorageKey().contains(cleanFilename))
                    .findFirst();

            if (fuzzyMatch.isPresent()) {
                Attachment attachment = fuzzyMatch.get();
                log.info("通过模糊查找找到附件 - id: {}, storageKey: {}",
                        attachment.getId(), attachment.getStorageKey());
                return attachment;
            }

            log.warn("未找到附件 - forumNo: {}, filename: {}", forumNo, filename);
            return null;

        } catch (Exception e) {
            log.error("查找附件异常", e);
            return null;
        }
    }

    /**
     * 根据附件ID获取附件
     */
    public Attachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("附件不存在: " + attachmentId));
    }

    /**
     * 获取文件的MIME类型
     */
    public String getFileMimeType(Attachment attachment) {
        try {
            Path filePath = Paths.get(getAttachmentPhysicalPath(attachment));
            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                // 根据文件扩展名判断
                String extension = getFileExtension(attachment.getOriginalName()).toLowerCase();

                return switch (extension) {
                    case "pdf" -> "application/pdf";
                    case "doc" -> "application/msword";
                    case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    case "ppt" -> "application/vnd.ms-powerpoint";
                    case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    case "xls" -> "application/vnd.ms-excel";
                    case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "png" -> "image/png";
                    case "gif" -> "image/gif";
                    case "txt" -> "text/plain";
                    case "zip" -> "application/zip";
                    case "rar" -> "application/x-rar-compressed";
                    default -> "application/octet-stream";
                };
            }
            return contentType;
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}