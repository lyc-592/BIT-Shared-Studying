package com.example.sharing.forum.service;

import com.example.sharing.forum.entity.Forum;
import com.example.sharing.forum.dto.ForumDTO;
import com.example.sharing.core.repository.CourseRepository;
import com.example.sharing.forum.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;

    /**
     * 根据课程号获取论坛信息（逻辑对象，不是数据库实体）
     */
    public Optional<Forum> getForumByCourseNo(Long courseNo) {
        // 验证课程是否存在
        if (!courseRepository.existsById(courseNo)) {
            return Optional.empty();
        }

        // 统计该课程下的话题数量
        Integer topicCount = topicRepository.countByForumNo(courseNo);

        // 创建论坛逻辑对象
        Forum forum = new Forum(courseNo, topicCount);
        return Optional.of(forum);
    }

    /**
     * 将Forum实体转换为ForumDTO
     */
    public ForumDTO convertToDTO(Forum forum, String courseName) {
        ForumDTO dto = new ForumDTO();
        dto.setForumNo(forum.getForumNo());
        dto.setCourseName(courseName);
        dto.setTopicCount(forum.getTopicCount());
        return dto;
    }
}