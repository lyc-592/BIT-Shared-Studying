package com.example.sharing.service;

import com.example.sharing.dto.CourseDto;
import com.example.sharing.dto.MajorDto;
import com.example.sharing.entity.Major;
import com.example.sharing.entity.MajorCourse;
import com.example.sharing.repository.MajorCourseRepository;
import com.example.sharing.repository.MajorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ⭐ 记得导这个包

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
     */
    @Transactional(readOnly = true)  // ⭐ 加上这一行
    public List<CourseDto> getCoursesByMajorNo(Long majorNo) {
        Major major = majorRepository.findById(majorNo)
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
}