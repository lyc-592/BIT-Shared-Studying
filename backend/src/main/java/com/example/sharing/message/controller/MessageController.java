package com.example.sharing.message.controller;

import com.example.sharing.dto.ApiResponse;
import com.example.sharing.message.dto.MessageDTO;
import com.example.sharing.message.dto.MessageDetailDTO;
import com.example.sharing.message.enums.MessageType;
import com.example.sharing.message.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 收件箱列表
     */
    @GetMapping("/inbox")
    public ApiResponse<Page<MessageDTO>> inbox(@RequestParam("userId") Long userId,
                                               @RequestParam(value = "type", required = false) MessageType type,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<MessageDTO> result = messageService.getInbox(userId, type, PageRequest.of(page, size));
        return ApiResponse.success("查询成功", result);
    }

    /**
     * 查看消息详情（同时标记为已读）
     */
    @GetMapping("/{id}")
    public ApiResponse<MessageDetailDTO> detail(@PathVariable("id") Long messageId,
                                                @RequestParam("userId") Long userId) {
        MessageDetailDTO detail = messageService.getMessageDetail(messageId, userId);
        return ApiResponse.success("查询成功", detail);
    }
}