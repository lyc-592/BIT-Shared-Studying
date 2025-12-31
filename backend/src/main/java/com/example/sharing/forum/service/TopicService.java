package com.example.sharing.forum.service;

import com.example.sharing.profile.UserProfile;
import com.example.sharing.profile.UserProfileDTO;
import com.example.sharing.profile.UserProfileRepository;
import com.example.sharing.core.dto.CourseDto;
import com.example.sharing.core.entity.User;
import com.example.sharing.forum.dto.*;
import com.example.sharing.forum.entity.Attachment;
import com.example.sharing.forum.entity.Topic;
import com.example.sharing.forum.entity.Status;
import com.example.sharing.forum.repository.TopicRepository;
import com.example.sharing.core.repository.CourseRepository;
import com.example.sharing.core.repository.UserRepository;
import com.example.sharing.core.service.FileStorageService;
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
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final CourseRepository courseRepository;
    private final FileStorageService fileStorageService;
    private final ForumFileService forumFileService;

    /**
     * 获取课程（论坛）下的所有话题（分页）- 包含作者资料，支持多种排序
     */
    public PageResponse<TopicDTO> getTopicsByForumNo(Long forumNo, int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Topic> topicPage = topicRepository.findByForumNoAndStatus(
                forumNo, Status.ACTIVE, pageable
        );

        return PageResponse.of(topicPage.map(this::convertToDTO));
    }

    /**
     * 获取话题详情 - 包含作者资料
     */
    @Transactional
    public Optional<TopicDTO> getTopicByIdWithAuthor(Long topicId) {
        Optional<Topic> topicOpt = topicRepository.findById(topicId);

        if (topicOpt.isPresent()) {
            // 增加浏览量
            topicRepository.incrementViewCount(topicId);

            Topic topic = topicOpt.get();
            // 重新查询以获取更新后的数据
            topic = topicRepository.findById(topicId).orElse(topic);
            return Optional.of(convertToDTO(topic));
        }

        return Optional.empty();
    }

    /**
     * 创建话题
     */
    @Transactional
    public TopicDTO createTopic(CreateTopicRequest request, Long userId) {
        // 验证课程（论坛）是否存在
        if (!courseRepository.existsById(request.getForumNo())) {
            throw new RuntimeException("课程（论坛）不存在: " + request.getForumNo());
        }

        // 1. 先获取或创建 UserProfile
        UserProfile userProfile = getOrCreateUserProfile(userId);

        if (userProfile == null) {
            throw new RuntimeException("无法创建用户资料，请确保用户存在");
        }

        if (StringUtils.hasText(request.getReferencePath())) {
            // 检查引用的文件是否存在
            if (! fileStorageService.exists(request.getReferencePath())) {
                throw new RuntimeException("引用的文件不存在或无法访问: " + request.getReferencePath());
            }
        }

        // 2. 创建话题 - 这里设置的是 user_id，应该对应 user_profile 表的 user_id
        Topic topic = new Topic();
        topic.setForumNo(request.getForumNo()); // 设置课程号（论坛号）
        // 重要：这里设置的是 UserProfile 中的 user_id（即 users 表的 id）
        topic.setUserId(userProfile.getUser().getId());
        topic.setCourse(courseRepository.findById(request.getForumNo()).get());
        topic.setUserProfile(userProfile);
        topic.setTitle(request.getTitle());
        topic.setContent(request.getContent());
        topic.setReferencePath(request.getReferencePath());

        topic.setStatus(Status.ACTIVE);
        // 先保存话题，获取 topicId
        Topic savedTopic = topicRepository.save(topic);

        // 4. 处理附件上传（在话题保存后，传入正确的 topicId）
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            try {
                List<Attachment> attachments = forumFileService.saveForumAttachments(
                        request.getAttachments(),
                        userId,
                        request.getForumNo(),
                        savedTopic.getId(),  // 使用保存后的话题ID
                        null // commentId为null
                );

                // 设置话题的附件列表
                if (attachments != null && !attachments.isEmpty()) {
                    savedTopic.setAttachments(attachments);
                    log.info("成功上传 {} 个附件到话题: {}", attachments.size(), savedTopic.getId());

                    // 重新保存话题以更新附件关系
                    savedTopic = topicRepository.save(savedTopic);
                }
            } catch (Exception e) {
                log.error("上传附件失败: {}", e.getMessage(), e);
                // 如果附件上传失败，可以选择删除已创建的话题
                // 或者继续使用已创建的话题（不包含附件）
            }
        }

        log.info("创建话题成功，topicId: {}, forumNo: {}, userId: {}",
                savedTopic.getId(), request.getForumNo(), userId);

        return convertToDTO(savedTopic);
    }

    /**
     * 获取或创建 UserProfile
     */
    private UserProfile getOrCreateUserProfile(Long userId) {
        try {
            // 首先尝试查找 UserProfile
            Optional<UserProfile> userProfileOpt = userProfileRepository.findByUser_Id(userId);

            if (userProfileOpt.isPresent()) {
                return userProfileOpt.get();
            }

            // 如果不存在，先查找 User
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

            // 创建默认的 UserProfile
            UserProfile newProfile = new UserProfile();
            newProfile.setUser(user);
            newProfile.setNickname(user.getUsername() != null ? user.getUsername() : "用户" + userId);
            newProfile.setBio("");
            newProfile.setMajor("");

            return userProfileRepository.save(newProfile);

        } catch (Exception e) {
            log.error("获取或创建用户资料失败，userId: {}", userId, e);
            throw new RuntimeException("用户资料处理失败: " + e.getMessage());
        }
    }


    /**
     * 更新话题 - 支持附件上传和删除
     */
    @Transactional
    public Optional<TopicDTO> updateTopic(Long topicId, UpdateTopicRequest request, Long userId) {
        Optional<Topic> topicOpt = topicRepository.findById(topicId);

        if (topicOpt.isPresent()) {
            Topic topic = topicOpt.get();

            // 检查权限（只能修改自己的话题）
            if (!topic.getUserId().equals(userId)) {
                throw new RuntimeException("无权修改此话题");
            }

            // 1. 处理要删除的附件
            if (StringUtils.hasText(request.getAttachmentIdsToDelete())) {
                try {
                    // 解析要删除的附件ID列表
                    List<Long> idsToDelete = parseAttachmentIds(request.getAttachmentIdsToDelete());

                    if (!idsToDelete.isEmpty()) {
                        // 从数据库和文件系统中删除附件
                        deleteAttachmentsFromDatabaseAndFilesystem(idsToDelete, userId);

                        // 从话题的附件列表中移除已删除的附件
                        removeDeletedAttachmentsFromTopic(topic, idsToDelete);

                        log.info("成功删除 {} 个附件: {}", idsToDelete.size(), idsToDelete);
                    }
                } catch (Exception e) {
                    log.error("处理要删除的附件ID列表失败: {}", e.getMessage(), e);
                    // 继续处理其他逻辑
                }
            }

            // 2. 更新文本字段
            if (request.getTitle() != null) {
                topic.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                topic.setContent(request.getContent());
            }
            if (request.getReferencePath() != null) {
                if (StringUtils.hasText(request.getReferencePath())) {
                    // 检查引用的文件是否存在
                    if (!fileStorageService.exists(request.getReferencePath())) {
                        throw new RuntimeException("引用的文件不存在或无法访问: " + request.getReferencePath());
                    }
                }
                topic.setReferencePath(request.getReferencePath());
            }

            // 3. 处理新上传的附件
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                try {
                    List<Attachment> newAttachments = forumFileService.saveForumAttachments(
                            request.getAttachments(),
                            userId,
                            topic.getForumNo(),
                            topicId,  // 使用当前话题ID
                            null // commentId为null
                    );

                    // 将新附件添加到话题的附件列表中
                    if (newAttachments != null && !newAttachments.isEmpty()) {
                        List<Attachment> currentAttachments = topic.getAttachments();
                        if (currentAttachments == null) {
                            currentAttachments = new ArrayList<>();
                        }
                        currentAttachments.addAll(newAttachments);
                        topic.setAttachments(currentAttachments);
                        log.info("成功上传 {} 个新附件到话题: {}", newAttachments.size(), topicId);
                    }
                } catch (Exception e) {
                    log.error("上传新附件失败: {}", e.getMessage(), e);
                    // 继续保存话题，即使附件上传失败
                }
            }

            Topic updated = topicRepository.save(topic);
            return Optional.of(convertToDTO(updated));
        }

        return Optional.empty();
    }

    /**
     * 删除话题（软删除）
     */
    @Transactional
    public boolean deleteTopic(Long topicId, Long userId) {
        Optional<Topic> topicOpt = topicRepository.findById(topicId);

        if (topicOpt.isPresent()) {
            Topic topic = topicOpt.get();

            // 检查权限
            if (!topic.getUserId().equals(userId)) {
                throw new RuntimeException("无权删除此话题");
            }

            // 先删除话题的所有附件
            List<Attachment> attachments = topic.getAttachments();
            if (attachments != null && !attachments.isEmpty()) {
                for (Attachment attachment : attachments) {
                    try {
                        forumFileService.deleteAttachment(attachment.getId(), userId);
                    } catch (Exception e) {
                        log.error("删除附件失败: attachmentId={}", attachment.getId(), e);
                        // 继续删除其他附件
                    }
                }
            }

            // 软删除话题
            topicRepository.updateStatus(topicId, Status.DELETED);

            log.info("删除话题成功，topicId: {}, userId: {}", topicId, userId);
            return true;
        }

        return false;
    }

    /**
     * 搜索话题 - 包含作者资料
     */
    public PageResponse<TopicDTO> searchTopicsWithAuthor(Long forumNo, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 使用Specification构建动态查询
        Specification<Topic> spec = buildSearchSpecification(forumNo, keyword);

        Page<Topic> topicPage = topicRepository.findAll(spec, pageable);

        return PageResponse.of(topicPage.map(this::convertToDTO));
    }

    /**
     * 获取用户创建的话题 - 按创建时间倒序
     */
    public PageResponse<TopicDTO> getUserTopics(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Topic> topicPage = topicRepository.findByUserIdAndStatus(
                userId, Status.ACTIVE, pageable
        );

        return PageResponse.of(topicPage.map(this::convertToDTO));
    }


    /**
     * 解析附件ID字符串（支持逗号分隔和JSON格式）
     */
    private List<Long> parseAttachmentIds(String attachmentIdsStr) {
        List<Long> ids = new ArrayList<>();

        if (!StringUtils.hasText(attachmentIdsStr)) {
            return ids;
        }

        String trimmed = attachmentIdsStr.trim();

        // 尝试解析为JSON数组
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

        // 解析为逗号分隔的格式
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
     * 从数据库和文件系统中删除附件
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
                // 继续处理其他附件
            }
        }
    }

    /**
     * 从话题的附件列表中移除已删除的附件
     */
    private void removeDeletedAttachmentsFromTopic(Topic topic, List<Long> deletedAttachmentIds) {
        if (topic.getAttachments() == null || topic.getAttachments().isEmpty()) {
            return;
        }

        // 使用迭代器安全地删除元素
        Iterator<Attachment> iterator = topic.getAttachments().iterator();
        while (iterator.hasNext()) {
            Attachment attachment = iterator.next();
            if (deletedAttachmentIds.contains(attachment.getId())) {
                iterator.remove();
                log.info("从话题附件列表中移除附件: attachmentId={}", attachment.getId());
            }
        }
    }

    /**
     * 构建搜索条件
     */
    private Specification<Topic> buildSearchSpecification(Long forumNo, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 状态条件：只查询活跃状态的话题
            predicates.add(criteriaBuilder.equal(root.get("status"), Status.ACTIVE));

            // 论坛（课程）号条件
            if (forumNo != null) {
                predicates.add(criteriaBuilder.equal(root.get("forumNo"), forumNo));
            }

            // 关键词搜索条件
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword + "%";
                Predicate titlePredicate = criteriaBuilder.like(root.get("title"), likePattern);
                Predicate contentPredicate = criteriaBuilder.like(root.get("content"), likePattern);
                predicates.add(criteriaBuilder.or(titlePredicate, contentPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 实体转DTO（包含作者资料）
     */
    private TopicDTO convertToDTO(Topic topic) {

        TopicDTO dto = TopicDTO.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .content(topic.getContent())
                .forumNo(topic.getForumNo()) // 改为论坛编号（课程号）
                .referencePath(topic.getReferencePath() != null ? topic.getReferencePath() : "")
                .viewCount(topic.getViewCount())
                .replyCount(topic.getReplyCount())
                .likeCount(topic.getLikeCount())
                .collectCount(topic.getCollectCount())
                .status(topic.getStatus())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();

        // 设置作者信息
        if (topic.getUserProfile() != null) {
            UserProfileDTO authorDTO = new UserProfileDTO();
            authorDTO.setUserId(topic.getUserId());

            // 从UserProfile获取信息
            authorDTO.setNickname(topic.getUserProfile().getNickname());
            authorDTO.setBio(topic.getUserProfile().getBio());
            authorDTO.setMajor(topic.getUserProfile().getMajor());

            // 从关联的User获取信息
            if (topic.getUserProfile().getUser() != null) {
                authorDTO.setUsername(topic.getUserProfile().getUser().getUsername());
                authorDTO.setEmail(topic.getUserProfile().getUser().getEmail());
                authorDTO.setRole(topic.getUserProfile().getUser().getRole());
            }

            dto.setAuthor(authorDTO);
        }

        if (topic.getCourse() != null) {
            CourseDto courseDto = new CourseDto(
                    topic.getCourse().getCourseNo(),
                    topic.getCourse().getCourseName()
            );
            dto.setCourse(courseDto);
        }

        // 设置附件信息
        if (topic.getAttachments() != null && !topic.getAttachments().isEmpty()) {
            List<AttachmentDTO> attachmentDTOs = topic.getAttachments().stream()
                    .map(this::convertAttachmentToDTO)
                    .collect(Collectors.toList());
            dto.setAttachments(attachmentDTOs);
        }


        return dto;
    }

    /**
     * 附件实体转DTO
     */
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
        // ========== 新增：设置预览相关字段 ==========
        setPreviewInfo(dto, attachment);
        return dto;
    }

    /**
     * 设置预览信息
     */
    private void setPreviewInfo(AttachmentDTO dto, Attachment attachment) {
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

    /**
     * 判断文件是否为图片
     */
    private boolean isImageFile(String fileType) {
        if (fileType == null) {
            return false;
        }
        return fileType.startsWith("image/");
    }

    /**
     * 判断文件是否可预览（图片、PDF、文本、Office文档）
     */
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

    /**
     * 获取预览类型
     */
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
     * 获取热门话题（按浏览量排序）- 可选论坛
     */
    public PageResponse<TopicDTO> getHotTopics(Long forumNo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("viewCount").descending().and(Sort.by("createdAt").descending()));

        if (forumNo != null) {
            Page<Topic> topicPage = topicRepository.findByForumNoAndStatus(
                    forumNo, Status.ACTIVE, pageable
            );
            return PageResponse.of(topicPage.map(this::convertToDTO));
        } else {
            // 构建Specification
            Specification<Topic> spec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), Status.ACTIVE);

            Page<Topic> topicPage = topicRepository.findAll(spec, pageable);
            return PageResponse.of(topicPage.map(this::convertToDTO));
        }
    }

    /**
     * 增加话题的点赞数
     */
    @Transactional
    public void incrementLikeCount(Long topicId) {
        topicRepository.incrementLikeCount(topicId);
    }

    /**
     * 减少话题的点赞数
     */
    @Transactional
    public void decrementLikeCount(Long topicId) {
        topicRepository.decrementLikeCount(topicId);
    }

    /**
     * 增加话题的收藏数
     */
    @Transactional
    public void incrementCollectCount(Long topicId) {
        topicRepository.incrementCollectCount(topicId);
    }

    /**
     * 减少话题的收藏数
     */
    @Transactional
    public void decrementCollectCount(Long topicId) {
        topicRepository.decrementCollectCount(topicId);
    }

    /**
     * 增加话题的评论数
     */
    @Transactional
    public void incrementReplyCount(Long topicId) {
        topicRepository.incrementReplyCount(topicId);
    }

    /**
     * 减少话题的评论数
     */
    @Transactional
    public void decrementReplyCount(Long topicId) {
        topicRepository.decrementReplyCount(topicId);
    }
}

