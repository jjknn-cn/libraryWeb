package com.example.library.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具：统一前后端日期格式。
 */
public final class Dates {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Dates() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME.format(time);
    }

    public static String formatMinute(LocalDateTime time) {
        return time == null ? null : MINUTE.format(time);
    }

    public static String format(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    /** 查询条件中的开始时间：yyyy-MM-dd -> 当天 00:00:00 */
    public static LocalDateTime startOfDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    /** 查询条件中的结束时间：yyyy-MM-dd -> 当天 23:59:59 */
    public static LocalDateTime endOfDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim()).atTime(23, 59, 59);
    }

    /** 解析 datetime-local 或 yyyy-MM-dd HH:mm:ss */
    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().replace('T', ' ');
        if (text.length() == 10) {
            return LocalDate.parse(text).atStartOfDay();
        }
        if (text.length() == 16) {
            text = text + ":00";
        }
        return LocalDateTime.parse(text, DATE_TIME);
    }
}
