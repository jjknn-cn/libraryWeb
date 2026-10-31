package com.example.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接工具类（沿用第一次作业的 DBUtil，读取 db.properties 配置）。
 */
public class DBUtil {

    private static String url;
    private static String username;
    private static String password;

    static {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("未找到 db.properties");
            }
            Properties properties = new Properties();
            properties.load(in);
            Class.forName(properties.getProperty("jdbc.driver"));
            url = properties.getProperty("jdbc.url");
            username = properties.getProperty("jdbc.username");
            password = properties.getProperty("jdbc.password");
        } catch (Exception e) {
            throw new ExceptionInInitializerError("数据库配置初始化失败：" + e.getMessage());
        }
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static void close(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
