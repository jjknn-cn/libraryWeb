package com.example.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通用 JDBC 工具类（沿用第一次作业的 JDBCTool 思路）。
 *
 * 针对本次数据库做了两点适配：
 * 1. 数据库列名就是实体字段名（驼峰），不再做驼峰转下划线；
 * 2. 反射时沿继承链收集字段，因此 Base 中的 id/createTime/updateTime 也能映射；
 * 3. transient、集合、自定义对象类型的字段不参与表的读写（如 Book.bookCopies）。
 */
public class JDBCTool {

    /** 实体类名 -> 表名（其他实体默认类名全小写） */
    private static final Map<String, String> TABLE_NAMES = new HashMap<>();

    static {
        TABLE_NAMES.put("Book", "book");
        TABLE_NAMES.put("BookCopy", "bookcopy");
        TABLE_NAMES.put("Loan", "loan");
        TABLE_NAMES.put("User", "user");
        // 本库把管理员/操作员/学生/教师统一存放在 user 单表中
        TABLE_NAMES.put("Admin", "user");
        TABLE_NAMES.put("Operator", "user");
        TABLE_NAMES.put("Student", "user");
        TABLE_NAMES.put("Teacher", "user");
        TABLE_NAMES.put("BookRecommend", "bookrecommend");
    }

    private JDBCTool() {
    }

    /** 获取表名：优先取映射表，否则类名全小写 */
    public static String getTableName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        String tableName = TABLE_NAMES.get(simpleName);
        return tableName != null ? tableName : simpleName.toLowerCase(Locale.ROOT);
    }

    /** 沿继承链收集可持久化字段（排除静态、transient、集合与自定义对象） */
    public static List<Field> getPersistentFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                Class<?> type = field.getType();
                if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                    continue;
                }
                if (!isScalar(type)) {
                    continue;
                }
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    private static boolean isScalar(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Boolean.class
                || type == Character.class
                || Number.class.isAssignableFrom(type)
                || type == LocalDateTime.class
                || type == LocalDate.class
                || type.isEnum();
    }

    /** ResultSet -> 对象列表 */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Field> fields = getPersistentFields(clazz);
            while (rs.next()) {
                T obj = newInstance(clazz);
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Field field = findField(fields, columnName);
                    if (field == null) {
                        continue;
                    }
                    Object value = readValue(rs, i, field.getType());
                    field.set(obj, value);
                }
                list.add(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException("结果集转换为对象列表失败", e);
        }
        return list;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("实体类 " + clazz.getName() + " 缺少无参构造方法", e);
        }
    }

    private static Field findField(List<Field> fields, String columnName) {
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(columnName)) {
                return field;
            }
        }
        return null;
    }

    /** 按目标字段类型读取列值（处理 bit(1)、DATETIME、DATE 等） */
    private static Object readValue(ResultSet rs, int index, Class<?> type) throws SQLException {
        Object value = rs.getObject(index);
        if (value == null) {
            return type.isPrimitive() ? defaultPrimitive(type) : null;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof Number n) {
                return n.intValue() == 1;
            }
            if (value instanceof byte[] bytes) {
                return bytes.length > 0 && bytes[0] != 0;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
        if (type == LocalDateTime.class) {
            if (value instanceof LocalDateTime dateTime) {
                return dateTime;
            }
            if (value instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime();
            }
            if (value instanceof java.sql.Date date) {
                return date.toLocalDate().atStartOfDay();
            }
            if (value instanceof java.util.Date date) {
                return new Timestamp(date.getTime()).toLocalDateTime();
            }
        }
        if (type == LocalDate.class) {
            if (value instanceof LocalDate localDate) {
                return localDate;
            }
            if (value instanceof java.sql.Date date) {
                return date.toLocalDate();
            }
            if (value instanceof Timestamp timestamp) {
                return timestamp.toLocalDateTime().toLocalDate();
            }
        }
        if (Number.class.isAssignableFrom(type)) {
            Number number = (Number) value;
            if (type == Integer.class) {
                return number.intValue();
            }
            if (type == Long.class) {
                return number.longValue();
            }
            if (type == Double.class) {
                return number.doubleValue();
            }
        }
        if (type == String.class) {
            return String.valueOf(value);
        }
        return value;
    }

    private static Object defaultPrimitive(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    /** 新增：INSERT 所有非空字段（包含 id） */
    public static <T> int save(T obj, Connection connection) {
        Class<?> clazz = obj.getClass();
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (Field field : getPersistentFields(clazz)) {
            Object value = getValue(obj, field);
            if (value == null) {
                continue;
            }
            if (columns.length() > 0) {
                columns.append(',');
                placeholders.append(',');
            }
            columns.append(field.getName());
            placeholders.append('?');
            params.add(value);
        }
        String sql = "INSERT INTO " + getTableName(clazz) + " (" + columns + ") VALUES (" + placeholders + ")";
        return executeUpdate(connection, sql, params.toArray());
    }

    /** 更新：UPDATE 所有字段（含 null），WHERE id = ? */
    public static <T> int update(T obj, Connection connection) {
        Class<?> clazz = obj.getClass();
        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Object id = null;
        for (Field field : getPersistentFields(clazz)) {
            Object value = getValue(obj, field);
            if ("id".equals(field.getName())) {
                id = value;
                continue;
            }
            if (setClause.length() > 0) {
                setClause.append(',');
            }
            setClause.append(field.getName()).append("=?");
            params.add(value);
        }
        params.add(id);
        String sql = "UPDATE " + getTableName(clazz) + " SET " + setClause + " WHERE id=?";
        return executeUpdate(connection, sql, params.toArray());
    }

    /** 删除：DELETE FROM 表 WHERE id = ? */
    public static <T> int delete(T obj, Connection connection) {
        Class<?> clazz = obj.getClass();
        Object id = getValue(obj, findIdField(clazz));
        return executeUpdate(connection, "DELETE FROM " + getTableName(clazz) + " WHERE id=?", id);
    }

    /** 按主键查询单个对象 */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) {
        List<T> list = queryList("SELECT * FROM " + getTableName(clazz) + " WHERE id=?", clazz, connection, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 通用查询列表 */
    public static <T> List<T> queryList(String sql, Class<T> clazz, Connection connection, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return resultSetToList(rs, clazz);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败：" + e.getMessage(), e);
        }
    }

    /** 通用查询单个对象 */
    public static <T> T queryOne(String sql, Class<T> clazz, Connection connection, Object... params) {
        List<T> list = queryList(sql, clazz, connection, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 通用增删改 */
    public static int executeUpdate(Connection connection, String sql, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("数据库操作失败：" + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value == null) {
                statement.setObject(i + 1, null);
            } else if (value instanceof LocalDateTime dateTime) {
                statement.setObject(i + 1, dateTime);
            } else if (value instanceof LocalDate localDate) {
                statement.setObject(i + 1, localDate);
            } else if (value instanceof Boolean bool) {
                // bit(1) / tinyint(1) 用 0/1 写入
                statement.setInt(i + 1, bool ? 1 : 0);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    private static Field findIdField(Class<?> clazz) {
        for (Field field : getPersistentFields(clazz)) {
            if ("id".equals(field.getName())) {
                return field;
            }
        }
        throw new IllegalArgumentException("实体 " + clazz.getName() + " 没有 id 字段");
    }

    private static Object getValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("读取字段值失败：" + field.getName(), e);
        }
    }
}
