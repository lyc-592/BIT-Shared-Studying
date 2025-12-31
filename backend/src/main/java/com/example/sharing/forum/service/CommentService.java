package com.example.sharing.forum.service;

import com.example.sharing.profile.UserProfile;
import com.example.sharing.profile.UserProfileDTO;
import com.example.sharing.profile.UserProfileRepository;
import com.example.sharing.core.entity.User;
import com.example.sharing.forum.dto.*;
import com.example.sharing.forum.entity.Attachment;
import com.example.sharing.forum.entity.Comment;
import com.example.sharing.forum.entity.Topic;
import com.example.sharing.forum.entity.Status;
import com.example.sharing.forum.repository.CommentRepository;
import com.example.sharing.forum.repository.TopicRepository;
import com.example.sharing.core.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ForumFileService forumFileService;

    /**
     * 根据ID获取评论详情
     */
    public CommentDTO getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在: " + commentId));

        // 获取二级评论数量
        Integer replyCount = 0;
        if (comment.getLevel() == 0) {
            long count = commentRepository.countByRootIdAndStatus(commentId, Status.ACTIVE);
            replyCount = (int) count;
        }

        return convertToCommentDTO(comment, replyCount);
    }

    /**
     * 获取话题的一级评论（分页），包含二级评论数量
     */
    public PageResponse<CommentDTO> getRootCommentsByTopicId(Long topicId, int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Comment> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("topicId"), topicId));
            predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));
            predicates.add(criteriaBuilder.equal(root.get("level"), 0));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Comment> commentPage = commentRepository.findAll(spec, pageable);

        // 批量获取每个一级评论的二级评论数量
        Map<Long, Long> replyCountMap = new HashMap<>();
        if (!commentPage.getContent().isEmpty()) {
            List<Long> rootIds = commentPage.getContent().stream()
                    .map(Comment::getId)
                    .collect(Collectors.toList());

            // 批量查询二级评论数量
            for (Long rootId : rootIds) {
                long count = commentRepository.countByRootIdAndStatus(rootId, Status.ACTIVE);
                replyCountMap.put(rootId, count);
            }
        }

        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(comment -> {
                    Long replyCount = replyCountMap.getOrDefault(comment.getId(), 0L);
                    return convertToCommentDTO(comment, replyCount.intValue());
                })
                .collect(Collectors.toList());

        return createPageResponse(commentDTOs, commentPage);
    }

    /**
     * 获取评论详情（使用CommentDetailDTO）
     */
    public CommentDetailDTO getCommentDetail(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在: " + commentId));

        CommentDetailDTO detailDTO = new CommentDetailDTO();

        // 获取评论基本信息（带回复数量）
        Integer replyCount = 0;
        if (comment.getLevel() == 0) {
            long count = commentRepository.countByRootIdAndStatus(commentId, Status.ACTIVE);
            replyCount = (int) count;
        }
        detailDTO.setComment(convertToCommentDTO(comment, replyCount));
        detailDTO.setReplyCount(replyCount);

        // 如果是一级评论，获取预览回复
        if (comment.getLevel() == 0 && replyCount > 0) {
            Pageable previewPageable = PageRequest.of(0, 3, Sort.by("createdAt").ascending());
            Specification<Comment> previewSpec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(criteriaBuilder.equal(root.get("rootId"), commentId));
                predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            Page<Comment> previewPage = commentRepository.findAll(previewSpec, previewPageable);
            List<CommentDTO> previewReplies = previewPage.getContent().stream()
                    .map(c -> convertToCommentDTO(c, 0))
                    .collect(Collectors.toList());

            detailDTO.setPreviewReplies(previewReplies);

            // 设置分页信息
            detailDTO.setReplyTotalElements((long) replyCount);
            detailDTO.setReplyTotalPages((int) Math.ceil((double) replyCount / detailDTO.getReplyPageSize()));
        }

        return detailDTO;
    }

    /**
     * 创建评论
     */
    @Transactional
    public CommentDTO createComment(CreateCommentRequest request, Long userId) {
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new RuntimeException("话题不存在: " + request.getTopicId()));

        if (topic.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("话题已关闭，无法评论");
        }

        UserProfile userProfile = getUserProfile(userId);

        Comment parentComment = null;
        Long targetUserId = null;
        String targetUsername = null;

        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在: " + request.getParentId()));

            if (parentComment.getStatus() != Status.ACTIVE) {
                throw new RuntimeException("无法回复已删除的评论");
            }

            targetUserId = parentComment.getUserId();
            if (parentComment.getUserProfile() != null &&
                    parentComment.getUserProfile().getUser() != null) {
                targetUsername = parentComment.getUserProfile().getUser().getUsername();
            }
        }

        Comment comment = new Comment();
        comment.setTopicId(request.getTopicId());
        comment.setUserId(userId);
        comment.setUserProfile(userProfile);

        String content = request.getContent();
        if (parentComment != null && targetUsername != null) {
            if (!content.contains("@" + targetUsername)) {
                content = "@" + targetUsername + " " + content;
            }
        }
        comment.setContent(content);
        comment.setStatus(Status.ACTIVE);

        if (parentComment != null) {
            comment.setParentId(parentComment.getId());
            comment.setLevel(1);

            if (parentComment.getLevel() == 0) {
                comment.setRootId(parentComment.getId());
            } else {
                comment.setRootId(parentComment.getRootId() != null ?
                        parentComment.getRootId() : parentComment.getParentId());
            }
        } else {
            comment.setParentId(null);
            comment.setLevel(0);
            comment.setRootId(null);
        }

        if (parentComment != null && comment.getRootId() != null) {
            long childCount = commentRepository.countByRootIdAndStatus(comment.getRootId(), Status.ACTIVE);
            comment.setSortOrder((int) childCount + 1);
        } else {
            comment.setSortOrder(0);
        }

        Comment savedComment = commentRepository.save(comment);

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            try {
                Long forumNo = topic.getForumNo();

                List<Attachment> attachments = forumFileService.saveForumAttachments(
                        request.getAttachments(),
                        userId,
                        forumNo,
                        request.getTopicId(),
                        savedComment.getId()
                );

                if (attachments != null && !attachments.isEmpty()) {
                    savedComment.setAttachments(attachments);
                    savedComment = commentRepository.save(savedComment);
                    log.info("成功上传 {} 个附件到评论: {}", attachments.size(), savedComment.getId());
                }
            } catch (Exception e) {
                log.error("上传附件失败: {}", e.getMessage(), e);
            }
        }

        topicRepository.incrementReplyCount(request.getTopicId());

        log.info("创建评论成功，commentId: {}, topicId: {}, userId: {}, level: {}",
                savedComment.getId(), request.getTopicId(), userId, savedComment.getLevel());

        CommentDTO dto = convertToCommentDTO(savedComment, 0);
        dto.setTargetUserId(targetUserId);
        dto.setTargetUsername(targetUsername);
        return dto;
    }

    /**
     * 获取一级评论的二级评论（分页）
     */
    public PageResponse<CommentDTO> getRepliesByRootId(Long rootId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        Specification<Comment> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("rootId"), rootId));
            predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));
            predicates.add(criteriaBuilder.equal(root.get("level"), 1));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Comment> commentPage = commentRepository.findAll(spec, pageable);

        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(comment -> {
                    Comment targetComment = comment.getParentId() != null ?
                            commentRepository.findById(comment.getParentId()).orElse(null) : null;

                    String targetUsername = null;
                    Long targetUserId = null;
                    if (targetComment != null && targetComment.getUserProfile() != null &&
                            targetComment.getUserProfile().getUser() != null) {
                        targetUsername = targetComment.getUserProfile().getUser().getUsername();
                        targetUserId = targetComment.getUserId();
                    }

                    CommentDTO dto = convertToCommentDTO(comment, 0);
                    dto.setTargetUserId(targetUserId);
                    dto.setTargetUsername(targetUsername);
                    return dto;
                })
                .collect(Collectors.toList());

        return createPageResponse(commentDTOs, commentPage);
    }

    /**
     * 核心转换方法：Comment -> CommentDTO
     */
    private CommentDTO convertToCommentDTO(Comment comment, Integer replyCount) {
        CommentDTO dto = CommentDTO.builder()
                .id(comment.getId())
                .topicId(comment.getTopicId())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .level(comment.getLevel())
                .likeCount(comment.getLikeCount())
                .replyCount(replyCount)  // 设置回复数量
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();

        // 设置作者信息
        if (comment.getUserProfile() != null) {
            UserProfileDTO authorDTO = new UserProfileDTO();
            authorDTO.setUserId(comment.getUserId());
            authorDTO.setNickname(comment.getUserProfile().getNickname());
            authorDTO.setBio(comment.getUserProfile().getBio());
            authorDTO.setMajor(comment.getUserProfile().getMajor());

            if (comment.getUserProfile().getUser() != null) {
                authorDTO.setUsername(comment.getUserProfile().getUser().getUsername());
                authorDTO.setEmail(comment.getUserProfile().getUser().getEmail());
                authorDTO.setRole(comment.getUserProfile().getUser().getRole());
            }

            dto.setAuthor(authorDTO);
        }

        // ========== 新增：设置附件信息 ==========
        if (comment.getAttachments() != null && !comment.getAttachments().isEmpty()) {
            List<AttachmentDTO> attachmentDTOs = comment.getAttachments().stream()
                    .map(this::convertAttachmentToDTO)
                    .collect(Collectors.toList());
            dto.setAttachments(attachmentDTOs);
        }

        return dto;
    }

    /**
     * 创建分页响应
     */
    private <T> PageResponse<T> createPageResponse(List<T> content, Page<?> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        return response;
    }

    // ========== 以下方法保持不变，但需要调整convertToDTO调用 ==========

    /**
     * 更新评论
     */
    @Transactional
    public Optional<CommentDTO> updateComment(Long commentId, UpdateCommentRequest request, Long userId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);

        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();

            if (!comment.getUserId().equals(userId)) {
                throw new RuntimeException("无权修改此评论");
            }

            if (comment.getStatus() != Status.ACTIVE) {
                throw new RuntimeException("评论已被删除，无法修改");
            }

            if (StringUtils.hasText(request.getAttachmentIdsToDelete())) {
                try {
                    List<Long> idsToDelete = parseAttachmentIds(request.getAttachmentIdsToDelete());
                    if (!idsToDelete.isEmpty()) {
                        deleteAttachmentsFromDatabaseAndFilesystem(idsToDelete, userId);
                        removeDeletedAttachmentsFromComment(comment, idsToDelete);
                        log.info("成功删除 {} 个附件: {}", idsToDelete.size(), idsToDelete);
                    }
                } catch (Exception e) {
                    log.error("处理要删除的附件ID列表失败: {}", e.getMessage(), e);
                }
            }

            comment.setContent(request.getContent());

            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                try {
                    Topic topic = topicRepository.findById(comment.getTopicId())
                            .orElseThrow(() -> new RuntimeException("话题不存在"));

                    List<Attachment> newAttachments = forumFileService.saveForumAttachments(
                            request.getAttachments(),
                            userId,
                            topic.getForumNo(),
                            comment.getTopicId(),
                            commentId
                    );

                    if (newAttachments != null && !newAttachments.isEmpty()) {
                        List<Attachment> currentAttachments = comment.getAttachments();
                        if (currentAttachments == null) {
                            currentAttachments = new ArrayList<>();
                        }
                        currentAttachments.addAll(newAttachments);
                        comment.setAttachments(currentAttachments);
                        log.info("成功上传 {} 个新附件到评论: {}", newAttachments.size(), commentId);
                    }
                } catch (Exception e) {
                    log.error("上传新附件失败: {}", e.getMessage(), e);
                }
            }

            Comment updated = commentRepository.save(comment);

            // 获取回复数量
            Integer replyCount = 0;
            if (updated.getLevel() == 0) {
                long count = commentRepository.countByRootIdAndStatus(updated.getId(), Status.ACTIVE);
                replyCount = (int) count;
            }

            return Optional.of(convertToCommentDTO(updated, replyCount));
        }

        return Optional.empty();
    }

    /**
     * 删除评论
     */
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);

        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();

            if (!comment.getUserId().equals(userId)) {
                throw new RuntimeException("无权删除此评论");
            }

            long childCount = commentRepository.countByParentIdAndStatus(commentId, Status.ACTIVE);
            if (childCount > 0) {
                commentRepository.updateStatus(commentId, Status.DELETED);
                log.info("评论标记为删除（有子评论），commentId: {}, userId: {}", commentId, userId);
            } else {
                List<Attachment> attachments = comment.getAttachments();
                if (attachments != null && !attachments.isEmpty()) {
                    for (Attachment attachment : attachments) {
                        try {
                            forumFileService.deleteAttachment(attachment.getId(), userId);
                        } catch (Exception e) {
                            log.error("删除附件失败: attachmentId={}", attachment.getId(), e);
                        }
                    }
                }

                commentRepository.updateStatus(commentId, Status.DELETED);
                log.info("评论删除成功，commentId: {}, userId: {}", commentId, userId);
            }

            if (comment.getStatus() == Status.ACTIVE) {
                topicRepository.decrementReplyCount(comment.getTopicId());
            }

            return true;
        }

        return false;
    }

    /**
     * 获取用户的评论列表
     */
    public PageResponse<CommentDTO> getUserComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> commentPage = commentRepository.findByUserIdAndStatus(userId, Status.ACTIVE, pageable);

        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(comment -> {
                    Integer replyCount = 0;
                    if (comment.getLevel() == 0) {
                        long count = commentRepository.countByRootIdAndStatus(comment.getId(), Status.ACTIVE);
                        replyCount = (int) count;
                    }
                    return convertToCommentDTO(comment, replyCount);
                })
                .collect(Collectors.toList());

        return createPageResponse(commentDTOs, commentPage);
    }

    /**
     * 搜索评论
     */
    public PageResponse<CommentDTO> searchComments(String keyword, Long userId, Long topicId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Comment> spec = buildSearchSpecification(keyword, userId, topicId);
        Page<Comment> commentPage = commentRepository.findAll(spec, pageable);

        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(comment -> {
                    Integer replyCount = 0;
                    if (comment.getLevel() == 0) {
                        long count = commentRepository.countByRootIdAndStatus(comment.getId(), Status.ACTIVE);
                        replyCount = (int) count;
                    }
                    return convertToCommentDTO(comment, replyCount);
                })
                .collect(Collectors.toList());

        return createPageResponse(commentDTOs, commentPage);
    }

    /**
     * 获取话题评论统计信息
     */
    public CommentStatsDTO getCommentStats(Long topicId) {
        CommentStatsDTO stats = new CommentStatsDTO();

        long totalComments = commentRepository.countByTopicIdAndStatus(topicId, Status.ACTIVE);
        stats.setTotalComments(totalComments);

        Specification<Comment> rootSpec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("topicId"), topicId));
            predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));
            predicates.add(criteriaBuilder.equal(root.get("level"), 0));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        long rootCount = commentRepository.count(rootSpec);
        stats.setRootCommentCount(rootCount);

        stats.setReplyCount(totalComments - rootCount);

        Optional<Comment> latestComment = commentRepository.findFirstByTopicIdAndStatusOrderByCreatedAtDesc(
                topicId, Status.ACTIVE);
        latestComment.ifPresent(comment ->
                stats.setLatestCommentTime(comment.getCreatedAt().toString()));

        Specification<Comment> hottestSpec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("topicId"), topicId));
            predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(0, 1, Sort.by("likeCount").descending());
        Page<Comment> hottestPage = commentRepository.findAll(hottestSpec, pageable);
        if (!hottestPage.getContent().isEmpty()) {
            Comment comment = hottestPage.getContent().get(0);
            Integer replyCount = 0;
            if (comment.getLevel() == 0) {
                long count = commentRepository.countByRootIdAndStatus(comment.getId(), Status.ACTIVE);
                replyCount = (int) count;
            }
            stats.setHottestComment(convertToCommentDTO(comment, replyCount));
        }

        return stats;
    }

    /**
     * 获取用户资料
     */
    private UserProfile getUserProfile(Long userId) {
        try {
            Optional<UserProfile> userProfileOpt = userProfileRepository.findByUser_Id(userId);

            if (userProfileOpt.isPresent()) {
                return userProfileOpt.get();
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

            UserProfile newProfile = new UserProfile();
            newProfile.setUser(user);
            newProfile.setNickname(user.getUsername() != null ? user.getUsername() : "用户" + userId);
            newProfile.setBio("");
            newProfile.setMajor("");

            return userProfileRepository.save(newProfile);

        } catch (Exception e) {
            log.error("获取用户资料失败，userId: {}", userId, e);
            throw new RuntimeException("用户资料处理失败: " + e.getMessage());
        }
    }

    /**
     * 解析附件ID字符串
     */
    private List<Long> parseAttachmentIds(String attachmentIdsStr) {
        List<Long> ids = new ArrayList<>();

        if (!StringUtils.hasText(attachmentIdsStr)) {
            return ids;
        }

        String trimmed = attachmentIdsStr.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Long[] array = objectMapper.readValue(trimmed, Long[].class);
                ids.addAll(Arrays.asList(array));
                return ids;
            } catch (Exception e) {
                log.warn("JSON解析失败，尝试逗号分隔格式");
            }
        }

        String[] idStrings = trimmed.split(",");
        for (String idStr : idStrings) {
            try {
                Long id = Long.parseLong(idStr.trim());
                ids.add(id);
            } catch (NumberFormatException e) {
                log.warn("无效的附件ID格式: {}", idStr);
            }
        }

        return ids;
    }

    /**
     * 删除附件
     */
    private void deleteAttachmentsFromDatabaseAndFilesystem(List<Long> attachmentIds, Long userId) {
        for (Long attachmentId : attachmentIds) {
            try {
                boolean deleted = forumFileService.deleteAttachment(attachmentId, userId);
                if (deleted) {
                    log.info("删除附件成功: attachmentId={}, userId={}", attachmentId, userId);
                }
            } catch (Exception e) {
                log.error("删除附件失败: attachmentId={}", attachmentId, e);
            }
        }
    }

    /**
     * 从评论的附件列表中移除已删除的附件
     */
    private void removeDeletedAttachmentsFromComment(Comment comment, List<Long> deletedAttachmentIds) {
        if (comment.getAttachments() == null || comment.getAttachments().isEmpty()) {
            return;
        }

        Iterator<Attachment> iterator = comment.getAttachments().iterator();
        while (iterator.hasNext()) {
            Attachment attachment = iterator.next();
            if (deletedAttachmentIds.contains(attachment.getId())) {
                iterator.remove();
                log.info("从评论附件列表中移除附件: attachmentId={}", attachment.getId());
            }
        }
    }

    /**
     * 构建搜索条件
     */
    private Specification<Comment> buildSearchSpecification(String keyword, Long userId, Long topicId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }

            if (topicId != null) {
                predicates.add(criteriaBuilder.equal(root.get("topicId"), topicId));
            }

            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                Predicate contentPredicate = criteriaBuilder.like(root.get("content"), likePattern);
                predicates.add(contentPredicate);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ========== 新增：附件实体转DTO方法 ==========
    private AttachmentDTO convertAttachmentToDTO(com.example.sharing.forum.entity.Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setOriginalName(attachment.getOriginalName());
        dto.setAccessUrl(attachment.getAccessUrl());
        dto.setFileType(attachment.getFileType());
        dto.setFileSize(attachment.getFileSize());
        dto.setStorageType(attachment.getStorageType());
        dto.setUserId(attachment.getUserId());
        dto.setTopicId(attachment.getTopicId());
        dto.setCommentId(attachment.getCommentId());
        dto.setCreatedAt(attachment.getCreatedAt());

        // 设置预览相关字段（可以参考TopicService的setPreviewInfo方法）
        setAttachmentPreviewInfo(dto, attachment);

        return dto;
    }

    private void setAttachmentPreviewInfo(AttachmentDTO dto, Attachment attachment) {
        String fileType = attachment.getFileType();

        // 判断是否为图片
        boolean isImage = isImageFile(fileType);
        dto.setIsImage(isImage);

        // 判断是否为可预览文件
        boolean isPreviewable = isPreviewableFile(fileType);
        dto.setIsPreviewable(isPreviewable);

        // 设置预览类型
        String previewType = getPreviewType(fileType);
        dto.setPreviewType(previewType);

        String previewUrl = buildPreviewUrl(attachment);
        dto.setPreviewUrl(previewUrl);

        // 3. 缩略图URL：只有图片类型才有缩略图
        if (isImage) {
            String thumbnailUrl = buildThumbnailUrl(attachment);
            dto.setThumbnailUrl(thumbnailUrl);
        } else {
            dto.setThumbnailUrl(null);
        }
    }

    /**
     * 构建预览URL
     * 使用路由：/api/attachments/preview/{forumNo}/{filename}
     */
    private static String buildPreviewUrl(Attachment attachment) {
        try {
            String accessUrl = attachment.getAccessUrl();
            if (accessUrl == null) {
                return null;
            }

            // 检查URL中是否包含/download/路径
            if (accessUrl.contains("/download/")) {
                // 直接替换/download/为/preview/
                return accessUrl.replaceFirst("/download/", "/preview/");
            } else {
                // 如果不是/download/路径，尝试从storageKey提取
                String storageKey = attachment.getStorageKey();
                if (storageKey != null) {
                    String[] parts = storageKey.split("/");
                    if (parts.length >= 2) {
                        String forumPart = parts[0];
                        Long forumNo = Long.parseLong(forumPart.replace("forum_", ""));
                        String filename = parts[parts.length - 1];

                        return ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/attachments/preview/{forumNo}/{filename}")
                                .buildAndExpand(forumNo, filename)
                                .toUriString();
                    }
                }
                return accessUrl;
            }

        } catch (Exception e) {
            log.error("构建预览URL失败，返回访问URL", e);
            return attachment.getAccessUrl();
        }
    }

    /**
     * 构建缩略图URL
     * 使用路由：/api/attachments/{attachmentId}/thumbnail
     */
    private static String buildThumbnailUrl(Attachment attachment) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/attachments/{attachmentId}/thumbnail")
                    .buildAndExpand(attachment.getId())
                    .toUriString();
        } catch (Exception e) {
            log.error("构建缩略图URL失败", e);
            return null;
        }
    }

    // ========== 新增：判断文件是否为图片 ==========
    private boolean isImageFile(String fileType) {
        if (fileType == null) {
            return false;
        }
        return fileType.startsWith("image/");
    }

    // ========== 新增：判断文件是否可预览 ==========
    private boolean isPreviewableFile(String fileType) {
        if (fileType == null) {
            return false;
        }

        // 图片
        if (fileType.startsWith("image/")) {
            return true;
        }

        // PDF
        if (fileType.equals("application/pdf")) {
            return true;
        }

        // 文本文件
        if (fileType.equals("text/plain") ||
                fileType.equals("text/html") ||
                fileType.equals("text/css") ||
                fileType.equals("text/javascript") ||
                fileType.equals("application/json") ||
                fileType.equals("application/xml")) {
            return true;
        }

        // Office文档
        if (fileType.equals("application/msword") ||
                fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                fileType.equals("application/vnd.ms-excel") ||
                fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                fileType.equals("application/vnd.ms-powerpoint") ||
                fileType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
            return true;
        }

        return false;
    }

    // ========== 新增：获取预览类型 ==========
    private String getPreviewType(String fileType) {
        if (fileType == null) {
            return "other";
        }

        if (fileType.startsWith("image/")) {
            return "image";
        } else if (fileType.equals("application/pdf")) {
            return "pdf";
        } else if (fileType.equals("text/plain") ||
                fileType.equals("text/html") ||
                fileType.equals("text/css") ||
                fileType.equals("text/javascript") ||
                fileType.equals("application/json") ||
                fileType.equals("application/xml")) {
            return "text";
        } else if (fileType.equals("application/msword") ||
                fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return "word";
        } else if (fileType.equals("application/vnd.ms-excel") ||
                fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return "excel";
        } else if (fileType.equals("application/vnd.ms-powerpoint") ||
                fileType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
            return "powerpoint";
        } else {
            return "other";
        }
    }

    /**
     * 增加评论点赞数
     */
    @Transactional
    public void incrementLikeCount(Long commentId) {
        commentRepository.incrementLikeCount(commentId);
    }

    /**
     * 减少评论点赞数
     */
    @Transactional
    public void decrementLikeCount(Long commentId) {
        commentRepository.decrementLikeCount(commentId);
    }
}