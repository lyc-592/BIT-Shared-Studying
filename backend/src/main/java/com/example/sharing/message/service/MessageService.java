package com.example.sharing.message.service;

import com.example.sharing.message.dto.AdminApplyDTO;
import com.example.sharing.message.dto.FileUploadRequestDTO;
import com.example.sharing.message.dto.MessageDTO;
import com.example.sharing.message.dto.MessageDetailDTO;
import com.example.sharing.message.entity.AdminApply;
import com.example.sharing.message.entity.FileUploadRequest;
import com.example.sharing.message.entity.Message;
import com.example.sharing.message.enums.MessageType;
import com.example.sharing.message.repository.AdminApplyRepository;
import com.example.sharing.message.repository.FileUploadRequestRepository;
import com.example.sharing.message.repository.MessageRepository;
import com.example.sharing.entity.User;
import com.example.sharing.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AdminApplyRepository adminApplyRepository;
    private final FileUploadRequestRepository fileUploadRequestRepository;
    private final AdminApplyService adminApplyService;
    private final FileUploadRequestService fileUploadRequestService;

    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          AdminApplyRepository adminApplyRepository,
                          FileUploadRequestRepository fileUploadRequestRepository,
                          AdminApplyService adminApplyService,
                          FileUploadRequestService fileUploadRequestService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.adminApplyRepository = adminApplyRepository;
        this.fileUploadRequestRepository = fileUploadRequestRepository;
        this.adminApplyService = adminApplyService;
        this.fileUploadRequestService = fileUploadRequestService;
    }

    @Transactional(readOnly = true)
    public Page<MessageDTO> getInbox(Long userId, MessageType type, Pageable pageable) {
        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Page<Message> page;
        if (type != null) {
            page = messageRepository.findByReceiverAndType(receiver, type, pageable);
        } else {
            page = messageRepository.findByReceiver(receiver, pageable);
        }

        return page.map(this::toDto); // 这里仍在事务里，懒加载可用
    }

    @Transactional
    public MessageDetailDTO getMessageDetail(Long messageId, Long currentUserId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("消息不存在"));

        boolean isReceiver = msg.getReceiver() != null
                && msg.getReceiver().getId().equals(currentUserId);
        boolean isSender = msg.getSender() != null
                && msg.getSender().getId().equals(currentUserId);

        // 只能是收件人或发件人查看
        if (!isReceiver && !isSender) {
            throw new RuntimeException("无权查看该消息");
        }

        // 只有收件人查看时才标记为已读
        if (isReceiver && !Boolean.TRUE.equals(msg.getIsRead())) {
            msg.setIsRead(true);
            msg.setReadAt(LocalDateTime.now());
            messageRepository.save(msg);
        }

        MessageDetailDTO detail = new MessageDetailDTO();
        detail.setMessage(toDto(msg));

        if (msg.getAdminApplyId() != null) {
            AdminApply apply = adminApplyRepository.findById(msg.getAdminApplyId()).orElse(null);
            AdminApplyDTO dto = adminApplyService.toDto(apply);
            detail.setAdminApply(dto);
        }

        if (msg.getFileUploadRequestId() != null) {
            FileUploadRequest req = fileUploadRequestRepository
                    .findById(msg.getFileUploadRequestId())
                    .orElse(null);
            FileUploadRequestDTO dto = fileUploadRequestService.toDto(req);
            detail.setFileUploadRequest(dto);
        }

        return detail;
    }

    private MessageDTO toDto(Message msg) {
        MessageDTO dto = new MessageDTO();
        dto.setId(msg.getId());
        if (msg.getSender() != null) {
            dto.setSenderId(msg.getSender().getId());
            dto.setSenderName(msg.getSender().getUsername());
        }
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setReceiverName(msg.getReceiver().getUsername());
        dto.setType(msg.getType());
        dto.setTitle(msg.getTitle());
        dto.setContent(msg.getContent());
        dto.setIsRead(msg.getIsRead());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }
}