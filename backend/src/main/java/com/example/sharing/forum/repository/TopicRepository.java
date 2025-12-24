package com.example.sharing.forum.repository;

import com.example.sharing.forum.entity.Topic;
import com.example.sharing.forum.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    // 根据论坛（课程）号和状态分页查询
    Page<Topic> findByForumNoAndStatus(Long forumNo, int status, Pageable pageable);

    // 根据用户ID和状态分页查询
    Page<Topic> findByUserIdAndStatus(Long userId, int status, Pageable pageable);

    // 更新状态
    @Modifying
    @Query("UPDATE Topic t SET t.status = :status WHERE t.id = :topicId")
    void updateStatus(@Param("topicId") Long topicId, @Param("status") int status);

    // 增加浏览量
    @Modifying
    @Query("UPDATE Topic t SET t.viewCount = t.viewCount + 1 WHERE t.id = :topicId")
    void incrementViewCount(@Param("topicId") Long topicId);

    // 增加点赞数
    @Modifying
    @Query("UPDATE Topic t SET t.likeCount = t.likeCount + 1 WHERE t.id = :topicId")
    void incrementLikeCount(@Param("topicId") Long topicId);

    // 减少点赞数
    @Modifying
    @Query("UPDATE Topic t SET t.likeCount = t.likeCount - 1 WHERE t.id = :topicId AND t.likeCount > 0")
    void decrementLikeCount(@Param("topicId") Long topicId);

    // 增加收藏数
    @Modifying
    @Query("UPDATE Topic t SET t.collectCount = t.collectCount + 1 WHERE t.id = :topicId")
    void incrementCollectCount(@Param("topicId") Long topicId);

    // 减少收藏数
    @Modifying
    @Query("UPDATE Topic t SET t.collectCount = t.collectCount - 1 WHERE t.id = :topicId AND t.collectCount > 0")
    void decrementCollectCount(@Param("topicId") Long topicId);

    // 增加评论数
    @Modifying
    @Query("UPDATE Topic t SET t.replyCount = t.replyCount + 1 WHERE t.id = :topicId")
    void incrementReplyCount(@Param("topicId") Long topicId);

    // 减少评论数
    @Modifying
    @Query("UPDATE Topic t SET t.replyCount = t.replyCount - 1 WHERE t.id = :topicId AND t.replyCount > 0")
    void decrementReplyCount(@Param("topicId") Long topicId);

    // 统计今日新增话题数
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.status = :status AND t.createdAt >= :startOfDay")
    long countTodayTopics(@Param("status") Status status, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(t) FROM Topic t WHERE t.status = 1 AND t.forumNo = :forumNo")
    Integer countByForumNo(Long forumNo);
}