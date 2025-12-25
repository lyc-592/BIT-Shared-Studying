package com.example.sharing.forum.repository;

import com.example.sharing.forum.entity.ForumUserAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumUserActionRepository extends JpaRepository<ForumUserAction, Long> {

    // 根据用户ID、操作类型、目标ID查找记录
    Optional<ForumUserAction> findByUserIdAndActionTypeAndTargetId(
            Long userId, Integer actionType, Long targetId);

    // 检查用户是否对目标执行了某种操作且未取消
    boolean existsByUserIdAndActionTypeAndTargetIdAndIsCancelFalse(
            Long userId, Integer actionType, Long targetId);

    // 统计某个目标的某种操作数量（未取消的）
    long countByTargetIdAndActionTypeAndIsCancelFalse(
            Long targetId, Integer actionType);

    // 获取用户的所有某种操作（分页）
    Page<ForumUserAction> findByUserIdAndActionTypeAndIsCancelFalse(
            Long userId, Integer actionType, Pageable pageable);

    // 获取用户对某个目标的所有操作
    List<ForumUserAction> findByUserIdAndTargetId(Long userId, Long targetId);

    // 取消用户对某个目标的某种操作
    @Modifying
    @Query("UPDATE ForumUserAction f SET f.isCancel = true " +
            "WHERE f.userId = :userId AND f.actionType = :actionType AND f.targetId = :targetId AND f.isCancel = false")
    int cancelAction(@Param("userId") Long userId,
                     @Param("actionType") Integer actionType,
                     @Param("targetId") Long targetId);

    // 重新激活已取消的操作
    @Modifying
    @Query("UPDATE ForumUserAction f SET f.isCancel = false " +
            "WHERE f.userId = :userId AND f.actionType = :actionType AND f.targetId = :targetId AND f.isCancel = true")
    int reactivateAction(@Param("userId") Long userId,
                         @Param("actionType") Integer actionType,
                         @Param("targetId") Long targetId);

    // 获取用户的粉丝
    @Query("SELECT f.userId FROM ForumUserAction f " +
            "WHERE f.targetId = :userId AND f.actionType = 4 AND f.isCancel = false")
    List<Long> findFollowersByUserId(@Param("userId") Long userId);

    // 获取用户关注的人
    @Query("SELECT f.targetId FROM ForumUserAction f " +
            "WHERE f.userId = :userId AND f.actionType = 4 AND f.isCancel = false")
    List<Long> findFollowingByUserId(@Param("userId") Long userId);

    // 批量检查用户对多个目标的操作状态
    @Query("SELECT f.targetId FROM ForumUserAction f " +
            "WHERE f.userId = :userId AND f.actionType = :actionType AND f.targetId IN :targetIds AND f.isCancel = false")
    List<Long> findTargetIdsByUserIdAndActionTypeAndTargetIds(
            @Param("userId") Long userId,
            @Param("actionType") Integer actionType,
            @Param("targetIds") List<Long> targetIds);

    Page<ForumUserAction> findByTargetIdAndActionTypeAndIsCancelFalse(
            Long targetId, Integer actionType, Pageable pageable);
}