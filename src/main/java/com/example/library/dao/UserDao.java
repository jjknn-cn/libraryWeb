package com.example.library.dao;

import com.example.library.entity.Admin;
import com.example.library.entity.Operator;
import com.example.library.entity.Student;
import com.example.library.entity.Teacher;
import com.example.library.entity.User;
import com.example.library.util.Dates;
import com.example.library.web.ApiException;
import com.example.util.DBUtil;
import com.example.util.JDBCTool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户数据访问：所有角色共用 user 表，按 role 字段映射为不同的子类。
 */
public class UserDao {

    private static final String COLUMNS = "id,createTime,updateTime,name,password,phone,role,major,loginTime,"
            + "permissionLevel,entryTime,duty,area,grade,college,title";

    public User findByName(String name) {
        return queryOne("SELECT " + COLUMNS + " FROM user WHERE name=?", name);
    }

    public User findById(String id) {
        return queryOne("SELECT " + COLUMNS + " FROM user WHERE id=?", id);
    }

    public boolean existsName(String name) {
        return count("SELECT COUNT(*) FROM user WHERE name=?", name) > 0;
    }

    public boolean existsPhone(String phone) {
        return count("SELECT COUNT(*) FROM user WHERE phone=?", phone) > 0;
    }

    public void insert(User user) {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                JDBCTool.save(user, connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("注册失败：" + e.getMessage());
        }
    }

    public void updateLoginTime(String id) {
        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = DBUtil.getConnection()) {
            JDBCTool.executeUpdate(connection, "UPDATE user SET loginTime=?,updateTime=? WHERE id=?", now, now, id);
        } catch (SQLException e) {
            throw new ApiException("更新登录时间失败：" + e.getMessage());
        }
    }

    /** 管理员用户查询：关键字模糊匹配姓名/手机号/专业/角色，按创建时间倒序 */
    public List<User> search(String keyword) {
        String sql = "SELECT " + COLUMNS + " FROM user WHERE 1=1";
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql += " AND (name LIKE ? OR phone LIKE ? OR major LIKE ? OR role LIKE ?)";
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql += " ORDER BY createTime DESC, name ASC";
        List<User> users = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new ApiException("查询用户失败：" + e.getMessage());
        }
        return users;
    }

    /** 专业列表（注册页下拉） */
    public List<String> listMajors() {
        List<String> majors = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT DISTINCT major FROM user WHERE major IS NOT NULL AND major<>'' ORDER BY major");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                majors.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new ApiException("查询专业失败：" + e.getMessage());
        }
        return majors;
    }

    public static Map<String, Object> toMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("role", user.getRole());
        map.put("phone", user.getPhone());
        map.put("major", user.getMajor());
        map.put("createTime", Dates.format(user.getCreateTime()));
        map.put("loginTime", Dates.format(user.getLoginTime()));
        if (user instanceof Admin admin) {
            map.put("permissionLevel", admin.getPermissionLevel());
            map.put("entryTime", Dates.format(admin.getEntryTime()));
        } else if (user instanceof Operator operator) {
            map.put("area", operator.getArea());
            map.put("duty", operator.getDuty());
        } else if (user instanceof Student student) {
            map.put("grade", student.getGrade());
            map.put("college", student.getCollege());
        } else if (user instanceof Teacher teacher) {
            map.put("college", teacher.getCollege());
            map.put("title", teacher.getTitle());
        }
        return map;
    }

    private User queryOne(String sql, Object... params) {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        } catch (SQLException e) {
            throw new ApiException("查询用户失败：" + e.getMessage());
        }
    }

    private long count(String sql, Object... params) {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new ApiException("统计用户失败：" + e.getMessage());
        }
    }

    /** 根据 role 列创建对应的 User 子类实例并填充所有字段 */
    private User mapUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        User user;
        if ("管理员".equals(role)) {
            user = new Admin();
        } else if ("操作员".equals(role)) {
            user = new Operator();
        } else if ("教师".equals(role)) {
            user = new Teacher();
        } else {
            user = new Student();
        }
        user.setId(rs.getString("id"));
        user.setCreateTime(toLocalDateTime(rs, "createTime"));
        user.setUpdateTime(toLocalDateTime(rs, "updateTime"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setRole(role);
        user.setMajor(rs.getString("major"));
        user.setLoginTime(toLocalDateTime(rs, "loginTime"));

        if (user instanceof Admin admin) {
            int level = rs.getInt("permissionLevel");
            admin.setPermissionLevel(rs.wasNull() ? null : level);
            if (rs.getDate("entryTime") != null) {
                admin.setEntryTime(rs.getDate("entryTime").toLocalDate());
            }
        } else if (user instanceof Operator operator) {
            operator.setArea(rs.getString("area"));
            operator.setDuty(rs.getString("duty"));
        } else if (user instanceof Student student) {
            student.setGrade(rs.getString("grade"));
            student.setCollege(rs.getString("college"));
        } else if (user instanceof Teacher teacher) {
            teacher.setCollege(rs.getString("college"));
            teacher.setTitle(rs.getString("title"));
        }
        return user;
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return Dates.parse(String.valueOf(value));
    }
}
