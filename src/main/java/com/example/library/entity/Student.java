package com.example.library.entity;

/**
 * 学生。
 */
public class Student extends User {

    /** 年级 */
    private String grade;

    /** 所属学院 */
    private String college;

    @Override
    public String[] hasPermission() {
        return new String[]{"借书", "还书", "个人借阅查询"};
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
}
