package com.example.sharing.dto;

public class MajorDto {

    private Long majorNo;
    private String majorName;

    public MajorDto() {
    }

    public MajorDto(Long majorNo, String majorName) {
        this.majorNo = majorNo;
        this.majorName = majorName;
    }

    public Long getMajorNo() {
        return majorNo;
    }

    public String getMajorName() {
        return majorName;
    }
}