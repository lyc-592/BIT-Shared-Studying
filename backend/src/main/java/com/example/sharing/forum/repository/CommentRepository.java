package com.example.sharing.forum.repository;

import com.example.sharing.forum.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    // 根据用户ID和状态查找评论（分页）
    Page<Comment> findByUserIdAndStatus(Long userId, int status, Pageable pageable);

    // 统计话题的评论数量
    long countByTopicIdAndStatus(Long topicId, int status);

    // 统计用户的评论数量
    long countByUserIdAndStatus(Long userId, int status);

    // 统计父评论下的子评论数量
    long countByParentIdAndStatus(Long parentId, int status);

    // 更新评论状态
    @Modifying
    @Query("UPDATE Comment c SET c.status = :status WHERE c.id = :commentId")
    void updateStatus(@Param("commentId") Long commentId, @Param("status") int status);

    // 增加点赞数
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    void incrementLikeCount(@Param("commentId") Long commentId);

    // 减少点赞数
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :commentId AND c.likeCount > 0")
    void decrementLikeCount(@Param("commentId") Long commentId);

    // 获取话题的最新评论
    Optional<Comment> findFirstByTopicIdAndStatusOrderByCreatedAtDesc(Long topicId, int status);

    // 修改：添加根据根评论ID统计的方法到Repository
    long countByRootIdAndStatus(Long rootId, int status);
}