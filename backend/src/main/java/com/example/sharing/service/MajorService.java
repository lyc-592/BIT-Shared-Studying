package com.example.sharing.service;

import com.example.sharing.dto.CourseDto;
import com.example.sharing.dto.MajorDto;
import com.example.sharing.entity.Major;
import com.example.sharing.entity.MajorCourse;
import com.example.sharing.repository.MajorCourseRepository;
import com.example.sharing.repository.MajorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MajorService {

    private final MajorRepository majorRepository;
    private final MajorCourseRepository majorCourseRepository;

    public MajorService(MajorRepository majorRepository,
                        MajorCourseRepository majorCourseRepository) {
        this.majorRepository = majorRepository;
        this.majorCourseRepository = majorCourseRepository;
    }

    public List<MajorDto> getAllMajors() {
        return majorRepository.findAll().stream()
                .map(m -> new MajorDto(m.getMajorNo(), m.getMajorName()))
                .collect(Collectors.toList());
    }

    /**
     * 根据专业号获取该专业下所有课程
     * 如果前端传入字符串 "all"，则返回所有课程
     */
    @Transactional(readOnly = true)
    public List<CourseDto> getCoursesByMajorNo(String majorNo) {
        // 特殊约定：majorNo = "all" 时，查询全部课程
        if ("all".equalsIgnoreCase(majorNo)) {
            return getAllCourses();
        }

        // 其他情况按原来的 Long 类型逻辑处理
        Long majorNoLong;
        try {
            majorNoLong = Long.parseLong(majorNo);
        } catch (NumberFormatException e) {
            throw new RuntimeException("专业编号格式错误");
        }

        Major major = majorRepository.findById(majorNoLong)
                .orElseThrow(() -> new RuntimeException("专业不存在"));

        List<MajorCourse> relations = majorCourseRepository.findByMajor(major);

        return relations.stream()
                .map(MajorCourse::getCourse)
                .distinct()
                .map(c -> new CourseDto(
                        c.getCourseNo(),
                        c.getCourseName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有课程（通过 MajorCourse 关联表聚合，避免直接改 repository）
     */
    @Transactional(readOnly = true)
    protected List<CourseDto> getAllCourses() {
        List<MajorCourse> relations = majorCourseRepository.findAll();

        return relations.stream()
                .map(MajorCourse::getCourse)
                .distinct()
                .map(c -> new CourseDto(
                        c.getCourseNo(),
                        c.getCourseName()
                ))
                .collect(Collectors.toList());
    }
}