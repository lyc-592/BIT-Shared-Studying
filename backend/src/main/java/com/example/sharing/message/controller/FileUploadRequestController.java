package com.example.sharing.message.controller;

import com.example.sharing.core.dto.ApiResponse;
import com.example.sharing.message.service.FileUploadRequestService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/file-upload-requests")
public class FileUploadRequestController {

    private final FileUploadRequestService fileUploadRequestService;

    public FileUploadRequestController(FileUploadRequestService fileUploadRequestService) {
        this.fileUploadRequestService = fileUploadRequestService;
    }

    /**
     * 普通用户提交文件上传申请
     *
     * form-data:
     * - requesterId: Long
     * - courseNo: Long
     * - file: MultipartFile
     * - remark: String (可选)
     */
    @PostMapping("/submit")
    public ApiResponse<Void> submit(@RequestParam("requesterId") Long requesterId,
                                    @RequestParam("courseNo") Long courseNo,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "remark", required = false) String remark,
                                    @RequestParam("targetAbsolutePath") String targetAbsolutePath) {
        return fileUploadRequestService.submitUploadRequest(
                requesterId, courseNo, file, remark, targetAbsolutePath
        );
    }

    @PostMapping("/approve")
    public ApiResponse<Void> approve(@RequestParam Long messageId,
                                     @RequestParam Long reviewerId) {
        return fileUploadRequestService.approve(messageId, reviewerId);
    }

    @PostMapping("/reject")
    public ApiResponse<Void> reject(@RequestParam Long messageId,
                                    @RequestParam Long reviewerId,
                                    @RequestParam(required = false) String reason) {
        return fileUploadRequestService.reject(messageId, reviewerId, reason);
    }

    // 预览某个上传申请对应的待审核文件
    @GetMapping("/{requestId}/preview")
    public ResponseEntity<Resource> previewPendingFile(@PathVariable Long requestId) {
        return fileUploadRequestService.previewPendingFile(requestId);
    }

    // 下载某个上传申请对应的待审核文件
    @GetMapping("/{requestId}/download")
    public ResponseEntity<Resource> downloadPendingFile(@PathVariable Long requestId) {
        return fileUploadRequestService.downloadPendingFile(requestId);
    }
}