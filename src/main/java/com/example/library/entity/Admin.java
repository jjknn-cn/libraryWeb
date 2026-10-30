package com.example.library.entity;

import java.time.LocalDate;

/**
 * 管理员。
 */
public class Admin extends User {

    /** 权限等级 */
    private Integer permissionLevel;

    /** 入职时间 */
    private LocalDate entryTime;

    @Override
    public String[] hasPermission() {
        return new String[]{"用户信息查询", "借阅记录查询", "借阅数据统计", "借书", "还书", "审阅推荐图书"};
    }

    public Integer getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(Integer permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public LocalDate getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDate entryTime) {
        this.entryTime = entryTime;
    }
}
