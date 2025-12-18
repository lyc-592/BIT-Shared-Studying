package com.example.sharing.controller;

import com.example.sharing.dto.ApiResponse;
import com.example.sharing.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    // 构造方法注入 Service
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传文件接口
     * form-data:
     *   file:      文件
     *   targetDir: 服务器上的目标目录（必须已存在）
     */
    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam("targetDir") String targetDir) {
        return fileStorageService.storeFile(file, targetDir);
    }

    /**
     * 创建目录接口
     * form-data:
     *   dir:       创建目标相对路径
     */
    @PostMapping("/create_dir")
    public ApiResponse<String> createDir(@RequestParam("dir") String dir) {
        return fileStorageService.createDir(dir);
    }

    /**
     * 删除目录/文件接口
     * form-data:
     *   dir:       删除目标相对路径
     */
    @PostMapping("/delete")
    public ApiResponse<String> deleteDir(@RequestParam("dir") String dir) {
        return fileStorageService.delete(dir);
    }

    /**
     * 下载文件接口（标准下载，浏览器会直接下载文件）
     * 参数：
     *   path: 要下载的文件路径（如：CS101/课件/第一章.pdf）
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("path") String path,
            HttpServletRequest request) {

        return fileStorageService.downloadFile(path, request, true);
    }

    /**
     * 预览文件接口（浏览器尝试直接打开文件，如PDF、图片）
     * 参数：
     *   path: 要预览的文件路径
     */
    @GetMapping("/preview")
    public ResponseEntity<Resource> previewFile(
            @RequestParam("path") String path,
            HttpServletRequest request) {

        ResponseEntity<Resource> response = fileStorageService.downloadFile(path, request, false);

        // 修改 Content-Disposition 为 inline，让浏览器尝试直接打开
        return ResponseEntity.ok()
                .contentType(response.getHeaders().getContentType())
                .header("Content-Disposition", "inline")
                .body(response.getBody());
    }

    /**
     * 获取文件信息接口
     * 参数：
     *   path: 文件路径
     */
    @GetMapping("/info")
    public ApiResponse<FileStorageService.FileInfo> getFileInfo(@RequestParam("path") String path) {
        return fileStorageService.getFileInfo(path);
    }
}