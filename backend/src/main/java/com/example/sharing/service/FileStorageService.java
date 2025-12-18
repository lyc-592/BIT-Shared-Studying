package com.example.sharing.service;

import com.example.sharing.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.*;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Service
public class FileStorageService {

    // ============ 内部类 ============

    public static class FileInfo {
        private String name;              // 文件名
        private String path;              // 相对路径
        private String fullPath;         // 完整路径
        private long size;               // 文件大小（字节）
        private String sizeFormatted;    // 格式化后的文件大小
        private long lastModified;       // 最后修改时间
        private String extension;        // 文件扩展名
        private String contentType;      // 文件类型

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getFullPath() { return fullPath; }
        public void setFullPath(String fullPath) { this.fullPath = fullPath; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public String getSizeFormatted() { return sizeFormatted; }
        public void setSizeFormatted(String sizeFormatted) { this.sizeFormatted = sizeFormatted; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    @Value("${file.storage.base-path}")
    private String basePath;

    /**
     * @param file         前端上传的文件
     * @param targetDirStr 前端指定的服务器目录（比如 /data/uploads 或 D:/data）
     */
    public ApiResponse<String> storeFile(MultipartFile file, String targetDirStr) {
        // 1. 基本检查
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("上传文件为空");
        }
        if (targetDirStr == null || targetDirStr.trim().isEmpty()) {
            return ApiResponse.fail("目标路径不能为空");
        }

        try {
            // 2. 目标目录
            Path targetDir = getFullPath(targetDirStr);

            // 3. 判断目录是否存在，并且是目录
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                return ApiResponse.fail("目标目录不存在：" + targetDir);
            }

            // 4. 生成文件名（这里直接使用原始文件名）
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                originalFilename = "unnamed";
            }
            Path targetFile = targetDir.resolve(sanitizeFilename(originalFilename)).normalize();

            // 5. 保存文件（存在则覆盖）
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            // 6. 返回成功，data 里带上最终存储路径
            return ApiResponse.success("文件上传成功", targetFile.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.fail("文件保存失败：" + e.getMessage());
        }
    }


    /**
     * 创建目录
     * @param relativePath 相对于基础路径的目录路径（如：CS101/课件/作业）
     */
    public ApiResponse<String> createDir(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return ApiResponse.fail("目录路径不能为空");
        }

        try {
            // 获取完整路径
            Path targetDir = getFullPath(relativePath);

            // 判断目录是否已存在
            if (Files.exists(targetDir)) {
                if (Files.isDirectory(targetDir)) {
                    return ApiResponse.fail("目标目录已存在：" + relativePath);
                } else {
                    return ApiResponse.fail("目标路径已存在且不是目录：" + relativePath);
                }
            }

            // 创建目录（包括所有不存在的父目录）
            Files.createDirectories(targetDir);
            return ApiResponse.success("目录创建成功", relativePath);

        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.fail("目录创建失败：" + e.getMessage());
        } catch (InvalidPathException e) {
            return ApiResponse.fail("非法路径：" + e.getMessage());
        } catch (SecurityException e) {
            return ApiResponse.fail("权限不足：" + e.getMessage());
        }
    }

    /**
     * 公共删除方法 - 删除单个文件或空目录
     * @param relativePath 相对于基础路径的文件或目录路径
     */
    public ApiResponse<String> delete(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return ApiResponse.fail("路径不能为空");
        }

        try {
            // 获取完整路径
            Path targetPath = getFullPath(relativePath);

            // 检查路径是否存在
            if (!Files.exists(targetPath)) {
                return ApiResponse.fail("路径不存在：" + relativePath);
            }

            // 判断是文件还是目录，调用相应的私有方法
            if (Files.isRegularFile(targetPath)) {
                return deleteFileInternal(targetPath, relativePath);
            } else if (Files.isDirectory(targetPath)) {
                return deleteEmptyDirectoryInternal(targetPath, relativePath);
            } else {
                return ApiResponse.fail("路径既不是文件也不是目录：" + relativePath);
            }

        } catch (InvalidPathException e) {
            return ApiResponse.fail("非法路径：" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.fail("删除失败：" + e.getMessage());
        } catch (SecurityException e) {
            return ApiResponse.fail("权限不足：" + e.getMessage());
        }
    }

    /**
     * 下载文件
     * @param relativePath 相对于基础路径的文件路径
     * @param request HTTP请求
     * @param download 是否为下载（true: 强制下载, false: 预览）
     * @return ResponseEntity<Resource> 响应实体
     */
    public ResponseEntity<Resource> downloadFile(String relativePath, HttpServletRequest request, boolean download) {
        try {
            // 1. 获取完整路径
            Path filePath = getFullPath(relativePath);

            // 2. 检查文件是否存在
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }

            // 3. 检查是否是文件（不是目录）
            if (!Files.isRegularFile(filePath)) {
                throw new AccessDeniedException("指定路径不是文件: " + relativePath);
            }

            // 4. 加载文件为 Resource
            Resource resource = new UrlResource(filePath.toUri());

            // 5. 检查资源是否可读
            if (!resource.exists() || !resource.isReadable()) {
                throw new AccessDeniedException("无法读取文件: " + relativePath);
            }

            // 6. 获取文件名
            String filename = extractFileName(filePath, relativePath);

            // 7. 获取内容类型
            String contentType = getMimeType(filePath);

            // 8. 记录下载（可选）
            if (download) {
                recordDownload(relativePath);
            }

            // 9. 对文件名进行编码，确保中文等特殊字符能正确显示
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            // 10. 构建响应实体
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("X-Content-Type-Options", "nosniff"); // 防止浏览器猜测内容类型

            if (download) {
                // 强制下载：设置 Content-Disposition 为 attachment
                builder.header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);

                // 对于下载，可以设置一些缓存控制头，防止浏览器缓存文件
                builder.header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .header("Expires", "0");
            } else {
                // 预览：设置 Content-Disposition 为 inline
                builder.header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);
            }

            return builder.body(resource);

        } catch (FileNotFoundException e) {
            throw new RuntimeException("文件未找到: " + e.getMessage());
        } catch (AccessDeniedException e) {
            throw new RuntimeException("访问被拒绝: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("文件操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件流（适用于大文件或需要自定义处理的情况）
     * @param relativePath 相对路径
     * @return 文件输入流
     */
    public Path getFilePath(String relativePath) {
        try {
            Path filePath = getFullPath(relativePath);

            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + relativePath);
            }

            if (!Files.isRegularFile(filePath)) {
                throw new AccessDeniedException("指定路径不是文件: " + relativePath);
            }

            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("获取文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的下载信息（不实际下载文件）
     */
    public ApiResponse<FileInfo> getFileInfo(String relativePath) {
        try {
            Path filePath = getFullPath(relativePath);

            if (!Files.exists(filePath)) {
                return ApiResponse.fail("文件不存在: " + relativePath);
            }

            if (!Files.isRegularFile(filePath)) {
                return ApiResponse.fail("指定路径不是文件: " + relativePath);
            }

            FileInfo fileInfo = new FileInfo();
            fileInfo.setName(filePath.getFileName().toString());
            fileInfo.setPath(relativePath);
            fileInfo.setFullPath(filePath.toString());
            fileInfo.setSize(Files.size(filePath));
            fileInfo.setSizeFormatted(formatFileSize(Files.size(filePath)));
            fileInfo.setLastModified(Files.getLastModifiedTime(filePath).toMillis());
            fileInfo.setExtension(getFileExtension(filePath.getFileName().toString()));
            fileInfo.setContentType(getMimeType(filePath));

            return ApiResponse.success("获取文件信息成功", fileInfo);

        } catch (Exception e) {
            return ApiResponse.fail("获取文件信息失败: " + e.getMessage());
        }
    }


    // ============ 私有辅助方法 ============

    /**
     * 将相对路径转换为完整路径，并进行安全检查
     */
    @org.jetbrains.annotations.NotNull
    private Path getFullPath(String relativePath) throws InvalidPathException {
        // 清理路径，防止路径遍历攻击
        String cleanPath = sanitizePath(relativePath);

        // 构建完整路径
        Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        Path fullPath = baseDir.resolve(cleanPath).normalize();

        // 安全检查：确保目标路径在基础目录内
        if (!fullPath.startsWith(baseDir)) {
            throw new InvalidPathException(relativePath, "路径越权访问");
        }

        return fullPath;
    }

    /**
     * 清理路径，防止路径遍历攻击
     */
    private String sanitizePath(String path) {
        if (path == null) return "";
        // 替换所有反斜杠为正斜杠
        path = path.replace('\\', '/');
        // 移除所有 "../" 和 "./"
        while (path.contains("../") || path.contains("./")) {
            path = path.replace("../", "").replace("./", "");
        }
        // 移除开头的斜杠
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * 清理文件名，防止路径注入
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        // 移除路径分隔符和特殊字符
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 私有方法：删除单个文件
     */
    private ApiResponse<String> deleteFileInternal(Path filePath, String relativePath) throws IOException {
        // 安全检查：防止删除关键系统文件（可选）
        if (isProtectedFile(filePath)) {
            return ApiResponse.fail("禁止删除受保护的文件");
        }

        // 删除文件
        Files.delete(filePath);

        System.out.println("文件删除成功：" + filePath);
        return ApiResponse.success("文件删除成功", relativePath);
    }

    /**
     * 私有方法：删除空目录
     */
    private ApiResponse<String> deleteEmptyDirectoryInternal(Path dirPath, String relativePath) throws IOException {
        // 安全检查：防止删除基础目录
        Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        if (dirPath.equals(baseDir)) {
            return ApiResponse.fail("不能删除基础目录");
        }

        // 检查目录是否为空
        if (!isDirectoryEmpty(dirPath)) {
            return ApiResponse.fail("目录非空，无法删除：" + relativePath);
        }

        // 安全检查：防止删除受保护目录（可选）
        if (isProtectedDirectory(dirPath)) {
            return ApiResponse.fail("禁止删除受保护的目录");
        }

        // 删除空目录
        Files.delete(dirPath);

        System.out.println("空目录删除成功：" + dirPath);
        return ApiResponse.success("空目录删除成功", relativePath);
    }

    /**
     * 检查目录是否为空
     */
    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    /**
     * 检查是否为受保护的文件（可扩展）
     */
    private boolean isProtectedFile(Path filePath) {
        // 这里可以添加逻辑，防止删除某些重要文件
        // 例如：配置文件、系统文件等
        String fileName = filePath.getFileName().toString();

        // 示例：禁止删除以 .config、.properties 结尾的文件
        return fileName.endsWith(".config") || fileName.endsWith(".properties");
    }

    /**
     * 检查是否为受保护的目录（可扩展）
     */
    private boolean isProtectedDirectory(Path dirPath) {
        // 这里可以添加逻辑，防止删除某些重要目录
        // 例如：系统目录、配置目录等

        Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        String relativePath = baseDir.relativize(dirPath).toString();

        // 示例：禁止删除根目录下的 system、config 目录
        return relativePath.equals("system") || relativePath.equals("config");
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 生成内容处置头（Content-Disposition）
     */
    private String getContentDisposition(Resource resource, String relativePath) {
        try {
            // 获取文件名
            String filename = resource.getFilename();
            if (filename == null) {
                // 从相对路径中提取文件名
                filename = Paths.get(relativePath).getFileName().toString();
            }

            // 对文件名进行编码，确保中文等特殊字符能正确显示
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            // 返回 Content-Disposition 头
            // inline: 浏览器尝试直接打开文件（如PDF、图片）
            // attachment: 浏览器直接下载文件
            return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename;
        } catch (Exception e) {
            return "attachment; filename=\"file\"";
        }
    }

    /**
     * 记录下载次数（如果需要统计的话）
     */
    private void recordDownload(String relativePath) {
        // 这里可以添加记录下载次数的逻辑
        // 例如：更新数据库中的下载次数，记录日志等

        System.out.println("文件被下载: " + relativePath + " 时间: " + java.time.LocalDateTime.now());

        // 示例：记录到日志文件
        try {
            Path logPath = Paths.get(basePath, "downloads.log");
            String logEntry = String.format("[%s] 下载: %s\n",
                    java.time.LocalDateTime.now(), relativePath);
            Files.write(logPath, logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 记录日志失败不影响下载功能
        }
    }

    /**
     * 获取文件的 MIME 类型
     */
    private String getMimeType(Path filePath) throws IOException {
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // 根据文件扩展名判断
                String fileName = filePath.getFileName().toString();
                String extension = getFileExtension(fileName).toLowerCase();

                switch (extension) {
                    case "pdf":
                        return "application/pdf";
                    case "doc":
                        return "application/msword";
                    case "docx":
                        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    case "ppt":
                        return "application/vnd.ms-powerpoint";
                    case "pptx":
                        return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    case "xls":
                        return "application/vnd.ms-excel";
                    case "xlsx":
                        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    case "jpg":
                    case "jpeg":
                        return "image/jpeg";
                    case "png":
                        return "image/png";
                    case "gif":
                        return "image/gif";
                    case "txt":
                        return "text/plain";
                    case "zip":
                        return "application/zip";
                    case "rar":
                        return "application/x-rar-compressed";
                    default:
                        return "application/octet-stream";
                }
            }
            return contentType;
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    /**
     * 提取文件名（从路径中）
     */
    private String extractFileName(Path filePath, String relativePath) {
        // 优先从文件路径中获取文件名
        String fileName = filePath.getFileName().toString();

        // 如果文件名有问题，从相对路径中提取
        if (fileName == null || fileName.isEmpty() || fileName.contains("?")) {
            // 从相对路径的最后一部分获取文件名
            String[] parts = relativePath.split("[\\\\/]");
            if (parts.length > 0) {
                fileName = parts[parts.length - 1];
            }
        }

        // 如果还是没有文件名，使用默认值
        if (fileName == null || fileName.isEmpty()) {
            fileName = "file";
        }

        return fileName;
    }
}