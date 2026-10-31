package com.example.library.dao;

import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.util.Dates;
import com.example.library.web.ApiException;
import com.example.util.DBUtil;
import com.example.util.JDBCTool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 借阅记录数据访问：借书、还书、借阅查询、借阅统计。
 */
public class LoanDao {

    private static final String BASE_SELECT = "SELECT l.id AS loanId,l.borrowDate,l.returnDate,l.dueDate,l.status,"
            + "c.id AS copyId,b.id AS bookId,b.title,b.author,b.type,b.days,"
            + "u.id AS userId,u.name AS userName,u.role AS userRole,u.major AS userMajor "
            + "FROM loan l JOIN bookcopy c ON c.id=l.bookCopyId JOIN book b ON b.id=c.bookId "
            + "JOIN user u ON u.id=l.userId ";

    /** 借书：选择一本可借副本，写入借阅记录，同时更新副本状态和库存 */
    public Map<String, Object> borrow(String userId, String bookId) {
        if (bookId == null || bookId.isBlank()) {
            throw new ApiException("请选择要借阅的图书");
        }
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book book = JDBCTool.queryOne("SELECT * FROM book WHERE id=? FOR UPDATE", Book.class, connection, bookId);
                if (book == null) {
                    throw new ApiException("图书不存在");
                }
                if ("已废弃".equals(book.getStatus())) {
                    throw new ApiException("该图书已废弃，不能借阅");
                }
                if (book.getStock() == null || book.getStock() <= 0) {
                    throw new ApiException("该图书暂无可借副本");
                }
                String copyId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM bookcopy WHERE bookId=? AND isBorrowed=0 ORDER BY createTime ASC LIMIT 1 FOR UPDATE")) {
                    statement.setString(1, bookId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new ApiException("该图书暂无可借副本（库存数据异常，请联系管理员）");
                        }
                        copyId = rs.getString("id");
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                int days = book.getDays() == null || book.getDays() <= 0 ? 30 : book.getDays();
                String loanId = UUID.randomUUID().toString();
                JDBCTool.executeUpdate(connection,
                        "INSERT INTO loan (id,createTime,updateTime,bookCopyId,userId,borrowDate,returnDate,dueDate,status) "
                                + "VALUES (?,?,?,?,?,?,NULL,?,'未还')",
                        loanId, now, now, copyId, userId, now, now.plusDays(days));
                JDBCTool.executeUpdate(connection, "UPDATE bookcopy SET isBorrowed=1,updateTime=? WHERE id=?", now, copyId);
                JDBCTool.executeUpdate(connection, "UPDATE book SET stock=stock-1,updateTime=? WHERE id=?", now, bookId);
                connection.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("loanId", loanId);
                result.put("bookTitle", book.getTitle());
                result.put("dueDate", Dates.format(now.plusDays(days)));
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("借书失败：" + e.getMessage());
        }
    }

    /** 还书：把副本恢复为未借出，库存 +1；学生/教师只能还自己的书 */
    public Map<String, Object> returnLoan(String loanId, User operator) {
        if (loanId == null || loanId.isBlank()) {
            throw new ApiException("请选择要归还的借阅记录");
        }
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String userId;
                String copyId;
                String bookId;
                String status;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT l.userId,l.status,c.id AS copyId,c.bookId FROM loan l "
                                + "JOIN bookcopy c ON c.id=l.bookCopyId WHERE l.id=? FOR UPDATE")) {
                    statement.setString(1, loanId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new ApiException("借阅记录不存在");
                        }
                        userId = rs.getString("userId");
                        status = rs.getString("status");
                        copyId = rs.getString("copyId");
                        bookId = rs.getString("bookId");
                    }
                }
                if ("已还".equals(status)) {
                    throw new ApiException("该借阅记录已经归还");
                }
                boolean manager = "管理员".equals(operator.getRole()) || "操作员".equals(operator.getRole());
                if (!manager && !operator.getId().equals(userId)) {
                    throw new ApiException("只能归还自己借阅的图书");
                }
                LocalDateTime now = LocalDateTime.now();
                JDBCTool.executeUpdate(connection,
                        "UPDATE loan SET returnDate=?,status='已还',updateTime=? WHERE id=?", now, now, loanId);
                JDBCTool.executeUpdate(connection, "UPDATE bookcopy SET isBorrowed=0,updateTime=? WHERE id=?", now, copyId);
                JDBCTool.executeUpdate(connection, "UPDATE book SET stock=stock+1,updateTime=? WHERE id=?", now, bookId);
                connection.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("loanId", loanId);
                result.put("returnDate", Dates.format(now));
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("还书失败：" + e.getMessage());
        }
    }

    /** 个人借阅查询 */
    public List<Map<String, Object>> mine(String userId, String status, String start, String end) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE l.userId=?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        appendStatusAndRange(sql, params, status, start, end);
        sql.append(" ORDER BY l.borrowDate DESC, l.createTime DESC");
        return queryMaps(sql.toString(), params);
    }

    /** 管理员借阅记录查询：关键字（用户名/书名）、超期、借阅日期范围 */
    public List<Map<String, Object>> query(String keyword, boolean overdue, String start, String end) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (u.name LIKE ? OR b.title LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (overdue) {
            sql.append(" AND l.status IN ('未还','超期') AND l.dueDate < ?");
            params.add(LocalDateTime.now());
        }
        LocalDateTime startTime = Dates.startOfDay(start);
        LocalDateTime endTime = Dates.endOfDay(end);
        if (startTime != null) {
            sql.append(" AND l.borrowDate >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sql.append(" AND l.borrowDate <= ?");
            params.add(endTime);
        }
        sql.append(" ORDER BY l.borrowDate DESC, l.createTime DESC");
        return queryMaps(sql.toString(), params);
    }

    /** 所有未归还记录（管理员/操作员还书页面使用） */
    public List<Map<String, Object>> active() {
        String sql = BASE_SELECT + "WHERE l.status IN ('未还','超期') ORDER BY l.dueDate ASC, l.borrowDate ASC";
        return queryMaps(sql, new ArrayList<>());
    }

    /** 按图书类型统计借阅次数（没有借阅记录的类型也显示 0） */
    public List<Map<String, Object>> statsByType() {
        String sql = "SELECT b.type AS name,COUNT(l.id) AS total FROM book b "
                + "LEFT JOIN bookcopy c ON c.bookId=b.id "
                + "LEFT JOIN loan l ON l.bookCopyId=c.id "
                + "WHERE b.type IS NOT NULL AND b.type<>'' "
                + "GROUP BY b.type ORDER BY total DESC, b.type ASC";
        return queryStats(sql);
    }

    /** 按用户专业统计借阅次数 */
    public List<Map<String, Object>> statsByMajor() {
        String sql = "SELECT u.major AS name,COUNT(l.id) AS total FROM user u "
                + "LEFT JOIN loan l ON l.userId=u.id "
                + "WHERE u.major IS NOT NULL AND u.major<>'' "
                + "GROUP BY u.major ORDER BY total DESC, u.major ASC";
        return queryStats(sql);
    }

    private void appendStatusAndRange(StringBuilder sql, List<Object> params, String status, String start, String end) {
        if ("unreturned".equalsIgnoreCase(status)) {
            sql.append(" AND l.status IN ('未还','超期')");
        }
        LocalDateTime startTime = Dates.startOfDay(start);
        LocalDateTime endTime = Dates.endOfDay(end);
        if (startTime != null) {
            sql.append(" AND l.borrowDate >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sql.append(" AND l.borrowDate <= ?");
            params.add(endTime);
        }
    }

    private List<Map<String, Object>> queryMaps(String sql, List<Object> params) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLoan(rs));
                }
            }
        } catch (SQLException e) {
            throw new ApiException("查询借阅记录失败：" + e.getMessage());
        }
        return list;
    }

    private List<Map<String, Object>> queryStats(String sql) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("name", rs.getString("name"));
                map.put("value", rs.getLong("total"));
                list.add(map);
            }
        } catch (SQLException e) {
            throw new ApiException("借阅统计失败：" + e.getMessage());
        }
        return list;
    }

    private Map<String, Object> mapLoan(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        String status = rs.getString("status");
        LocalDateTime dueDate = toLocalDateTime(rs.getObject("dueDate"));
        LocalDateTime now = LocalDateTime.now();
        boolean overdue = !"已还".equals(status) && dueDate != null && dueDate.isBefore(now);
        if (overdue) {
            status = "超期";
        }
        map.put("loanId", rs.getString("loanId"));
        map.put("copyId", rs.getString("copyId"));
        map.put("bookId", rs.getString("bookId"));
        map.put("title", rs.getString("title"));
        map.put("author", rs.getString("author"));
        map.put("type", rs.getString("type"));
        map.put("days", rs.getObject("days"));
        map.put("userId", rs.getString("userId"));
        map.put("userName", rs.getString("userName"));
        map.put("userRole", rs.getString("userRole"));
        map.put("userMajor", rs.getString("userMajor"));
        map.put("borrowDate", Dates.format(toLocalDateTime(rs.getObject("borrowDate"))));
        map.put("dueDate", Dates.format(dueDate));
        map.put("returnDate", Dates.format(toLocalDateTime(rs.getObject("returnDate"))));
        map.put("status", status);
        map.put("overdue", overdue);
        map.put("overdueDays", overdue ? Duration.between(dueDate, now).toDays() : 0);
        return map;
    }

    private LocalDateTime toLocalDateTime(Object value) {
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
