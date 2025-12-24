package com.example.sharing.forum.repository;

import com.example.sharing.forum.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * 根据话题ID查找所有附件（话题主贴的附件）
     */
    List<Attachment> findByTopicId(Long topicId);

    /**
     * 根据话题ID查找所有附件，并按创建时间排序
     */
    List<Attachment> findByTopicIdOrderByCreatedAtAsc(Long topicId);

    /**
     * 根据话题ID查找所有附件，并按创建时间倒序排序
     */
    List<Attachment> findByTopicIdOrderByCreatedAtDesc(Long topicId);

    /**
     * 根据评论ID查找所有附件（评论的附件）
     */
    List<Attachment> findByCommentId(Long commentId);

    /**
     * 根据用户ID查找用户上传的所有附件
     */
    List<Attachment> findByUserId(Long userId);

    /**
     * 根据用户ID和话题ID查找附件
     */
    List<Attachment> findByUserIdAndTopicId(Long userId, Long topicId);

    /**
     * 根据论坛编号（forumNo）查找附件
     * 需要联表查询，因为Attachment没有直接存储forumNo
     */
    @Query("SELECT a FROM Attachment a WHERE a.topicId IN " +
            "(SELECT t.id FROM Topic t WHERE t.forumNo = :forumNo)")
    List<Attachment> findByForumNo(@Param("forumNo") Long forumNo);

    /**
     * 根据话题ID列表批量查找附件
     */
    List<Attachment> findByTopicIdIn(List<Long> topicIds);

    /**
     * 根据评论ID列表批量查找附件
     */
    List<Attachment> findByCommentIdIn(List<Long> commentIds);

    /**
     * 根据存储标识（storageKey）查找附件
     */
    Optional<Attachment> findByStorageKey(String storageKey);

    /**
     * 根据访问URL查找附件
     */
    Optional<Attachment> findByAccessUrl(String accessUrl);

    /**
     * 根据原始文件名查找附件
     */
    List<Attachment> findByOriginalName(String originalName);

    /**
     * 根据文件类型查找附件
     */
    List<Attachment> findByFileType(String fileType);

    /**
     * 统计话题的附件数量
     */
    long countByTopicId(Long topicId);

    /**
     * 统计评论的附件数量
     */
    long countByCommentId(Long commentId);

    /**
     * 统计用户的附件数量
     */
    long countByUserId(Long userId);

    /**
     * 根据话题ID删除所有附件（软删除，逻辑删除）
     */
    @Transactional
    @Modifying
    @Query("UPDATE Attachment a SET a.storageType = 'deleted' WHERE a.topicId = :topicId")
    int softDeleteByTopicId(@Param("topicId") Long topicId);

    /**
     * 根据话题ID永久删除所有附件（物理删除）
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.topicId = :topicId")
    int deleteByTopicId(@Param("topicId") Long topicId);

    /**
     * 根据评论ID永久删除所有附件（物理删除）
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.commentId = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * 根据用户ID和话题ID删除附件
     */
    @Transactional
    @Modifying
    int deleteByUserIdAndTopicId(Long userId, Long topicId);

    /**
     * 根据存储标识删除附件
     */
    @Transactional
    @Modifying
    int deleteByStorageKey(String storageKey);

    /**
     * 查找指定文件大小范围内的附件
     */
    @Query("SELECT a FROM Attachment a WHERE a.fileSize BETWEEN :minSize AND :maxSize")
    List<Attachment> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * 查找大于指定文件大小的附件
     */
    List<Attachment> findByFileSizeGreaterThan(Long fileSize);

    /**
     * 查找小于指定文件大小的附件
     */
    List<Attachment> findByFileSizeLessThan(Long fileSize);

    /**
     * 根据存储类型查找附件
     */
    List<Attachment> findByStorageType(String storageType);

    /**
     * 查找最近创建的附件
     */
    List<Attachment> findTop10ByOrderByCreatedAtDesc();

    /**
     * 查找用户最近上传的附件
     */
    List<Attachment> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 统计指定话题的附件总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM Attachment a WHERE a.topicId = :topicId")
    Long sumFileSizeByTopicId(@Param("topicId") Long topicId);

    /**
     * 统计指定用户的附件总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM Attachment a WHERE a.userId = :userId")
    Long sumFileSizeByUserId(@Param("userId") Long userId);

    /**
     * 查找有附件的话题ID列表
     */
    @Query("SELECT DISTINCT a.topicId FROM Attachment a WHERE a.topicId IS NOT NULL")
    List<Long> findTopicIdsWithAttachments();

    /**
     * 查找有附件的评论ID列表
     */
    @Query("SELECT DISTINCT a.commentId FROM Attachment a WHERE a.commentId IS NOT NULL")
    List<Long> findCommentIdsWithAttachments();

    /**
     * 查找指定文件类型和用户ID的附件
     */
    List<Attachment> findByFileTypeAndUserId(String fileType, Long userId);

    /**
     * 批量删除附件（根据ID列表）
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Attachment a WHERE a.id IN :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    /**
     * 更新附件的访问URL
     */
    @Transactional
    @Modifying
    @Query("UPDATE Attachment a SET a.accessUrl = :accessUrl WHERE a.id = :id")
    int updateAccessUrl(@Param("id") Long id, @Param("accessUrl") String accessUrl);

    /**
     * 更新附件的存储类型
     */
    @Transactional
    @Modifying
    @Query("UPDATE Attachment a SET a.storageType = :storageType WHERE a.id = :id")
    int updateStorageType(@Param("id") Long id, @Param("storageType") String storageType);

    /**
     * 检查附件是否存在（根据存储标识）
     */
    boolean existsByStorageKey(String storageKey);

    /**
     * 检查用户是否有指定话题的附件
     */
    boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    /**
     * 查找未关联任何话题或评论的"孤儿"附件
     */
    @Query("SELECT a FROM Attachment a WHERE a.topicId IS NULL AND a.commentId IS NULL")
    List<Attachment> findOrphanAttachments();

    /**
     * 查找指定时间段内创建的附件
     */
    @Query("SELECT a FROM Attachment a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<Attachment> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 根据话题ID列表查找所有附件，并按话题分组
     * 返回一个Map，key为topicId，value为该话题的附件列表
     */
    @Query("SELECT a FROM Attachment a WHERE a.topicId IN :topicIds")
    List<Attachment> findByTopicIdInGrouped(@Param("topicIds") List<Long> topicIds);

    @Query("SELECT a FROM Attachment a WHERE a.storageKey LIKE %:keyword%")
    Optional<Attachment> findByStorageKeyContaining(@Param("keyword") String keyword);
}