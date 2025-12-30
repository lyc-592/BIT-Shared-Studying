package com.example.sharing.message.controller;

import com.example.sharing.core.dto.ApiResponse;
import com.example.sharing.message.service.AdminApplyService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/admin-apply")
public class AdminApplyController {

    private final AdminApplyService adminApplyService;

    public AdminApplyController(AdminApplyService adminApplyService) {
        this.adminApplyService = adminApplyService;
    }

    /**
     * 普通用户提交管理员申请
     *
     * form-data:
     * - applicantId: Long
     * - majorNo: Long
     * - wordFile: MultipartFile
     * - remark: String (可选)
     */
    @PostMapping("/submit")
    public ApiResponse<Void> submit(@RequestParam("applicantId") Long applicantId,
                                    @RequestParam("majorNo") Long majorNo,
                                    @RequestPart("wordFile") MultipartFile wordFile,
                                    @RequestParam(value = "remark", required = false) String remark) {
        return adminApplyService.submitApply(applicantId, majorNo, wordFile, remark);
    }

    /**
     * 审核通过（管理员）
     *
     * 前端在“消息详情”里点击同意时，传 messageId + reviewerId
     */
    @PostMapping("/approve")
    public ApiResponse<Void> approve(@RequestParam("messageId") Long messageId,
                                     @RequestParam("reviewerId") Long reviewerId) {
        return adminApplyService.approve(messageId, reviewerId);
    }

    /**
     * 审核拒绝（管理员）
     */
    @PostMapping("/reject")
    public ApiResponse<Void> reject(@RequestParam("messageId") Long messageId,
                                    @RequestParam("reviewerId") Long reviewerId,
                                    @RequestParam(value = "reason", required = false) String reason) {
        return adminApplyService.reject(messageId, reviewerId, reason);
    }



    /**
     * 下载管理员申请的 Word 简历
     * GET /api/admin-apply/{applyId}/word/download
     */
    @GetMapping("/{applyId}/word/download")
    public ResponseEntity<Resource> downloadWord(@PathVariable Long applyId) {
        return adminApplyService.downloadWord(applyId);
    }
}