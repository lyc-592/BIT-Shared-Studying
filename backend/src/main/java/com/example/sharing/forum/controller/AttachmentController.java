package com.example.sharing.forum.controller;

import com.example.sharing.forum.entity.Attachment;
import com.example.sharing.forum.service.ForumFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final ForumFileService forumFileService;

    /**
     * 通过唯一文件名下载附件（主入口）
     */
    @GetMapping("/download/{forumNo}/{filename}")
    public ResponseEntity<Resource> downloadByFilename(
            @PathVariable Long forumNo,
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "false") Boolean download) {

        log.info("收到下载请求 - forumNo: {}, filename: {}", forumNo, filename);

        try {
            // 1. 查找附件
            Attachment attachment = forumFileService.findByForumNoAndFilename(forumNo, filename);
            if (attachment == null) {
                log.warn("附件未找到");
                return ResponseEntity.notFound().build();
            }

            log.info("找到附件 - id: {}, originalName: {}, storageKey: {}",
                    attachment.getId(), attachment.getOriginalName(), attachment.getStorageKey());

            // 2. 获取物理路径
            String filePathStr = forumFileService.getAttachmentPhysicalPath(attachment);
            Path filePath = Paths.get(filePathStr);

            if (!Files.exists(filePath)) {
                log.error("物理文件不存在: {}", filePathStr);
                return ResponseEntity.notFound().build();
            }

            // 3. 创建Resource
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("资源不可读");
                return ResponseEntity.status(503).build();
            }

            // 4. 设置响应头
            HttpHeaders headers = new HttpHeaders();
            long fileSize = Files.size(filePath);
            String contentType = forumFileService.getFileMimeType(attachment);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 设置Content-Disposition（Spring 5.0+内置支持中文文件名）
            ContentDisposition contentDisposition = ContentDisposition.builder(download ? "attachment" : "inline")
                    .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                    .build();

            headers.setContentDisposition(contentDisposition);

            // 缓存控制
            if (download) {
                headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                headers.add(HttpHeaders.PRAGMA, "no-cache");
                headers.add(HttpHeaders.EXPIRES, "0");
            }

            // 5. 返回响应
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException e) {
            log.error("IO异常", e);
            return ResponseEntity.status(503).build();
        } catch (Exception e) {
            log.error("下载附件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 通过附件ID下载（需要权限验证）
     */
    @GetMapping("/download-by-id/{attachmentId}")
    public ResponseEntity<Resource> downloadById(
            @PathVariable Long attachmentId,
            @RequestParam(required = false, defaultValue = "false") Boolean download,
            HttpServletRequest request) {

        try {
            // 1. 查找附件
            Attachment attachment = forumFileService.getAttachmentById(attachmentId);

            // 2. 获取附件的物理路径
            String filePathStr = forumFileService.getAttachmentPhysicalPath(attachment);
            Path filePath = Paths.get(filePathStr);

            // 3. 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.warn("物理文件不存在，但数据库记录存在: attachmentId={}, path={}",
                        attachmentId, filePathStr);
                return ResponseEntity.notFound().build();
            }

            // 4. 创建Resource对象
            Resource resource = new UrlResource(filePath.toUri());

            // 5. 判断是下载还是预览
            boolean isDownload = Boolean.TRUE.equals(download);
            String contentType = forumFileService.getFileMimeType(attachment);

            // 6. 设置响应头
            HttpHeaders headers = new HttpHeaders();

            // 使用附件的原始文件名
            String displayName = attachment.getOriginalName();

            if (isDownload) {
                headers.setContentDispositionFormData("attachment", displayName);
                headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                headers.add(HttpHeaders.PRAGMA, "no-cache");
                headers.add(HttpHeaders.EXPIRES, "0");
            } else {
                headers.setContentDispositionFormData("inline", displayName);
            }

            // 7. 返回文件
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(Files.size(filePath))
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            log.error("通过ID下载附件失败: attachmentId={}", attachmentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 预览附件（内联显示）
     */
    @GetMapping("/preview/{forumNo}/{filename}")
    public ResponseEntity<Resource> previewByFilename(
            @PathVariable Long forumNo,
            @PathVariable String filename,
            HttpServletRequest request) {
        log.info("预览附件 - forumNo: {}, filename: {}", forumNo, filename);
        // 直接调用下载方法，设置download=false
        return downloadByFilename(forumNo, filename, false);
    }

    /**
     * 判断文件类型是否可在浏览器中预览
     */
    private boolean isBrowserPreviewable(String contentType) {
        if (contentType == null) {
            return false;
        }

        // 图片
        if (contentType.startsWith("image/")) {
            return true;
        }

        // PDF
        if (contentType.equals("application/pdf")) {
            return true;
        }

        // 文本文件
        if (contentType.startsWith("text/")) {
            return true;
        }

        // JSON、XML
        if (contentType.equals("application/json") ||
                contentType.equals("application/xml") ||
                contentType.equals("application/javascript")) {
            return true;
        }

        return false;
    }

}