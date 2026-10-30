package com.example.library.entity;

import java.time.LocalDateTime;

/**
 * 用户抽象基类。数据库中所有角色共用 user 表，通过 role 字段区分。
 */
public abstract class User extends Base {

    /** 姓名（登录用户名） */
    private String name;

    /** 密码 */
    private String password;

    /** 手机号 */
    private String phone;

    /** 角色：管理员、操作员、学生、教师 */
    private String role;

    /** 专业 */
    private String major;

    /** 最后一次登录时间 */
    private LocalDateTime loginTime;

    /** 抽象方法：获取用户可执行的操作权限列表 */
    public abstract String[] hasPermission();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
