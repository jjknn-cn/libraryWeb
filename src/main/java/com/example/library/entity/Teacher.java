package com.example.library.entity;

/**
 * 教师。
 */
public class Teacher extends User {

    /** 所属学院 */
    private String college;

    /** 职称 */
    private String title;

    @Override
    public String[] hasPermission() {
        return new String[]{"借书", "还书", "个人借阅查询", "推荐图书"};
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
