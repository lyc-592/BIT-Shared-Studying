package com.example.sharing.forum.controller;

import com.example.sharing.core.dto.ApiResponse;
import com.example.sharing.forum.dto.AttachmentPreviewInfoDTO;
import com.example.sharing.forum.entity.Attachment;
import com.example.sharing.forum.service.ForumFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentPreviewController {

    private final ForumFileService forumFileService;

    /**
     * 获取附件的预览信息
     */
    @GetMapping("/{attachmentId}/preview-info")
    public ApiResponse<AttachmentPreviewInfoDTO> getPreviewInfo(@PathVariable Long attachmentId) {
        try {
            Attachment attachment = forumFileService.getAttachmentById(attachmentId);
            AttachmentPreviewInfoDTO previewInfo = convertToPreviewInfoDTO(attachment);
            return ApiResponse.success("获取预览信息成功", previewInfo);
        } catch (Exception e) {
            log.error("获取预览信息失败: attachmentId={}", attachmentId, e);
            return ApiResponse.fail("获取预览信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片的Base64编码（用于直接在前端显示）
     * 适合小图片，大图片建议使用直接访问URL
     */
    @GetMapping(value = "/{attachmentId}/base64", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> getImageAsBase64(@PathVariable Long attachmentId) {
        try {
            Attachment attachment = forumFileService.getAttachmentById(attachmentId);

            // 检查是否为图片
            if (!attachment.getFileType().startsWith("image/")) {
                return ApiResponse.fail("只能预览图片文件");
            }

            // 检查文件大小，避免处理大文件
            if (attachment.getFileSize() > 5 * 1024 * 1024) { // 5MB
                Map<String, Object> result = new HashMap<>();
                result.put("warning", "文件过大，建议使用直接访问URL");
                result.put("accessUrl", attachment.getAccessUrl());
                result.put("fileSize", attachment.getFileSize());
                result.put("fileType", attachment.getFileType());
                return ApiResponse.success("文件过大，请使用直接访问URL", result);
            }

            // 读取文件并转换为Base64
            String physicalPath = forumFileService.getAttachmentPhysicalPath(attachment);
            Path filePath = Paths.get(physicalPath);

            if (!Files.exists(filePath)) {
                return ApiResponse.fail("文件不存在");
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String base64 = Base64.getEncoder().encodeToString(fileContent);
            String dataUri = "data:" + attachment.getFileType() + ";base64," + base64;

            Map<String, Object> result = new HashMap<>();
            result.put("base64", dataUri);
            result.put("fileName", attachment.getOriginalName());
            result.put("fileType", attachment.getFileType());
            result.put("fileSize", attachment.getFileSize());
            result.put("isImage", true);

            return ApiResponse.success("获取图片Base64成功", result);
        } catch (Exception e) {
            log.error("获取图片Base64失败: attachmentId={}", attachmentId, e);
            return ApiResponse.fail("获取图片Base64失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片的缩略图（Base64格式）
     */
    @GetMapping(value = "/{attachmentId}/thumbnail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> getThumbnail(
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "200") Integer maxWidth,
            @RequestParam(defaultValue = "200") Integer maxHeight) {

        try {
            Attachment attachment = forumFileService.getAttachmentById(attachmentId);

            if (!attachment.getFileType().startsWith("image/")) {
                return ApiResponse.fail("只能为图片生成缩略图");
            }

            String physicalPath = forumFileService.getAttachmentPhysicalPath(attachment);
            Path filePath = Paths.get(physicalPath);

            if (!Files.exists(filePath)) {
                return ApiResponse.fail("文件不存在");
            }

            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(filePath.toFile());
            if (originalImage == null) {
                return ApiResponse.fail("无法读取图片文件");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 计算缩略图尺寸
            float ratio = Math.min(
                    (float) maxWidth / originalWidth,
                    (float) maxHeight / originalHeight
            );

            int thumbnailWidth = (int) (originalWidth * ratio);
            int thumbnailHeight = (int) (originalHeight * ratio);

            // 创建缩略图
            BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
            g.dispose();

            // 转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = getImageFormatName(attachment.getFileType());
            ImageIO.write(thumbnail, formatName, baos);
            byte[] thumbnailBytes = baos.toByteArray();

            String base64 = Base64.getEncoder().encodeToString(thumbnailBytes);
            String dataUri = "data:image/" + formatName + ";base64," + base64;

            Map<String, Object> result = new HashMap<>();
            result.put("thumbnail", dataUri);
            result.put("originalWidth", originalWidth);
            result.put("originalHeight", originalHeight);
            result.put("thumbnailWidth", thumbnailWidth);
            result.put("thumbnailHeight", thumbnailHeight);
            result.put("fileName", attachment.getOriginalName());
            result.put("fileSize", attachment.getFileSize());

            return ApiResponse.success("获取缩略图成功", result);

        } catch (Exception e) {
            log.error("生成缩略图失败: attachmentId={}", attachmentId, e);

            // 失败时返回原图URL
            try {
                Attachment attachment = forumFileService.getAttachmentById(attachmentId);
                Map<String, Object> result = new HashMap<>();
                result.put("accessUrl", attachment.getAccessUrl());
                result.put("warning", "缩略图生成失败，使用原图");
                return ApiResponse.success("缩略图生成失败，使用原图", result);
            } catch (Exception ex) {
                return ApiResponse.fail("缩略图生成失败: " + e.getMessage());
            }
        }
    }

    /**
     * 获取文件的文本内容（适用于文本文件）
     */
    @GetMapping("/{attachmentId}/text-content")
    public ApiResponse<Map<String, Object>> getTextContent(
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "10000") Integer maxLength) {

        try {
            Attachment attachment = forumFileService.getAttachmentById(attachmentId);

            // 检查是否为文本文件
            if (!isTextFile(attachment.getFileType())) {
                return ApiResponse.fail("只能预览文本文件");
            }

            // 检查文件大小
            if (attachment.getFileSize() > 1024 * 1024) { // 1MB
                return ApiResponse.fail("文件过大，不支持在线预览");
            }

            String physicalPath = forumFileService.getAttachmentPhysicalPath(attachment);
            Path filePath = Paths.get(physicalPath);

            if (!Files.exists(filePath)) {
                return ApiResponse.fail("文件不存在");
            }

            // 读取文件内容
            String content = Files.readString(filePath);

            // 限制内容长度
            if (content.length() > maxLength) {
                content = content.substring(0, maxLength) + "...\n\n[内容过长，已截断]";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("fileName", attachment.getOriginalName());
            result.put("fileType", attachment.getFileType());
            result.put("fileSize", attachment.getFileSize());
            result.put("truncated", content.length() > maxLength);

            return ApiResponse.success("获取文本内容成功", result);

        } catch (Exception e) {
            log.error("读取文本内容失败: attachmentId={}", attachmentId, e);
            return ApiResponse.fail("读取文本内容失败: " + e.getMessage());
        }
    }

    /**
     * 将Attachment转换为预览信息DTO
     */
    private AttachmentPreviewInfoDTO convertToPreviewInfoDTO(Attachment attachment) {
        AttachmentPreviewInfoDTO dto = new AttachmentPreviewInfoDTO();
        dto.setId(attachment.getId());
        dto.setOriginalName(attachment.getOriginalName());
        dto.setAccessUrl(attachment.getAccessUrl());
        dto.setFileType(attachment.getFileType());
        dto.setFileSize(attachment.getFileSize());
        dto.setCreatedAt(attachment.getCreatedAt());

        // 设置预览信息
        String fileType = attachment.getFileType();
        dto.setIsImage(fileType != null && fileType.startsWith("image/"));
        dto.setIsPdf("application/pdf".equals(fileType));
        dto.setIsText(isTextFile(fileType));
        dto.setIsOffice(isOfficeFile(fileType));

        return dto;
    }

    /**
     * 获取图片格式名
     */
    private String getImageFormatName(String fileType) {
        if (fileType == null) {
            return "jpeg";
        }

        if (fileType.equals("image/png")) {
            return "png";
        } else if (fileType.equals("image/gif")) {
            return "gif";
        } else {
            return "jpeg"; // 默认jpeg
        }
    }

    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(String fileType) {
        if (fileType == null) {
            return false;
        }

        return fileType.startsWith("text/") ||
                fileType.equals("application/json") ||
                fileType.equals("application/xml") ||
                fileType.equals("application/javascript");
    }

    /**
     * 判断是否为Office文件
     */
    private boolean isOfficeFile(String fileType) {
        if (fileType == null) {
            return false;
        }

        return fileType.contains("word") ||
                fileType.contains("excel") ||
                fileType.contains("powerpoint") ||
                fileType.contains("msword") ||
                fileType.contains("spreadsheet") ||
                fileType.contains("presentation");
    }
}