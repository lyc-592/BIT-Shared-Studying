package com.example.sharing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "major")
public class Major {

    // 专业号：主键，纯数字（Long）
    @Id
    @Column(name = "major_no")
    private Long majorNo;

    // 专业名称
    @Column(name = "major_name", nullable = false, length = 255)
    private String majorName;

    public Major() {
    }

    public Major(Long majorNo, String majorName) {
        this.majorNo = majorNo;
        this.majorName = majorName;
    }

    public Long getMajorNo() {
        return majorNo;
    }

    public void setMajorNo(Long majorNo) {
        this.majorNo = majorNo;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }
}