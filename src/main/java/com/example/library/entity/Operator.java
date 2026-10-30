package com.example.library.entity;

/**
 * 操作员。
 */
public class Operator extends User {

    /** 负责区域 */
    private String area;

    /** 职务 */
    private String duty;

    @Override
    public String[] hasPermission() {
        return new String[]{"添加书籍", "查看所有书籍", "废弃图书", "编辑图书", "图书副本编辑",
                "借书", "还书", "个人借阅查询"};
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDuty() {
        return duty;
    }

    public void setDuty(String duty) {
        this.duty = duty;
    }
}
