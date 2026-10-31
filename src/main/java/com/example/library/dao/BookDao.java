package com.example.library.dao;

import com.example.library.entity.Book;
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
import java.util.UUID;

/**
 * 图书与图书副本的数据访问。
 */
public class BookDao {

    /** 模糊查询图书；keyword 为空时查询全部，按创建时间倒序（新书在最前） */
    public List<Book> list(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM book WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendKeyword(sql, params, keyword);
        sql.append(" ORDER BY createTime DESC, title ASC");
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.queryList(sql.toString(), Book.class, connection, params.toArray());
        } catch (SQLException e) {
            throw new ApiException("查询图书失败：" + e.getMessage());
        }
    }

    /** 可借图书：状态正常且库存大于 0 */
    public List<Book> available(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM book WHERE status='正常' AND stock>0");
        List<Object> params = new ArrayList<>();
        appendKeyword(sql, params, keyword);
        sql.append(" ORDER BY createTime DESC, title ASC");
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.queryList(sql.toString(), Book.class, connection, params.toArray());
        } catch (SQLException e) {
            throw new ApiException("查询可借图书失败：" + e.getMessage());
        }
    }

    public Book findById(String id) {
        try (Connection connection = DBUtil.getConnection()) {
            return JDBCTool.getOneById(id, Book.class, connection);
        } catch (SQLException e) {
            throw new ApiException("查询图书失败：" + e.getMessage());
        }
    }

    /** 新增图书：按 stock 数量自动创建副本 */
    public void add(Book book) {
        if (book.getStock() == null || book.getStock() < 0) {
            throw new ApiException("库存数量不能为负数");
        }
        LocalDateTime now = LocalDateTime.now();
        book.setId(UUID.randomUUID().toString());
        book.setCreateTime(now);
        book.setUpdateTime(now);
        if (book.getStatus() == null || book.getStatus().isBlank()) {
            book.setStatus("正常");
        }
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                JDBCTool.save(book, connection);
                insertCopies(connection, book.getId(), book.getStock(), now);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("添加图书失败：" + e.getMessage());
        }
    }

    /** 编辑图书：只更新 title/author/type/publisher/days/price，不修改库存、副本和状态 */
    public void updateInfo(Book book) {
        try (Connection connection = DBUtil.getConnection()) {
            int rows = JDBCTool.executeUpdate(connection,
                    "UPDATE book SET title=?,author=?,type=?,publisher=?,days=?,price=?,updateTime=? WHERE id=?",
                    book.getTitle(), book.getAuthor(), book.getType(), book.getPublisher(),
                    book.getDays(), book.getPrice(), LocalDateTime.now(), book.getId());
            if (rows == 0) {
                throw new ApiException("图书不存在");
            }
        } catch (SQLException e) {
            throw new ApiException("编辑图书失败：" + e.getMessage());
        }
    }

    /** 废弃图书：必须所有副本都已归还 */
    public void discard(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book book = JDBCTool.queryOne("SELECT * FROM book WHERE id=? FOR UPDATE", Book.class, connection, bookId);
                if (book == null) {
                    throw new ApiException("图书不存在");
                }
                if ("已废弃".equals(book.getStatus())) {
                    throw new ApiException("该图书已经是废弃状态");
                }
                int borrowed = countBorrowedCopies(connection, bookId);
                if (borrowed > 0) {
                    throw new ApiException("该图书还有 " + borrowed + " 本副本未归还，不能废弃");
                }
                LocalDateTime now = LocalDateTime.now();
                JDBCTool.executeUpdate(connection, "UPDATE book SET status='已废弃',updateTime=? WHERE id=?", now, bookId);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (SQLException e) {
            throw new ApiException("废弃图书失败：" + e.getMessage());
        }
    }

    /** 查询某本图书的所有副本（附当前借阅人） */
    public List<Map<String, Object>> copies(String bookId) {
        String sql = "SELECT c.id,c.createTime,c.isBorrowed,u.name AS userName,l.dueDate "
                + "FROM bookcopy c "
                + "LEFT JOIN loan l ON l.bookCopyId=c.id AND l.status IN ('未还','超期') "
                + "LEFT JOIN user u ON u.id=l.userId "
                + "WHERE c.bookId=? ORDER BY c.createTime ASC, c.id ASC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", rs.getString("id"));
                    map.put("createTime", Dates.format(toLocalDateTime(rs.getObject("createTime"))));
                    map.put("borrowed", rs.getBoolean("isBorrowed"));
                    map.put("userName", rs.getString("userName"));
                    map.put("dueDate", Dates.format(toLocalDateTime(rs.getObject("dueDate"))));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new ApiException("查询副本失败：" + e.getMessage());
        }
        return list;
    }

    /** 新增一个副本，库存 +1 */
    public void addCopy(String bookId) {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Book book = JDBCTool.queryOne("SELECT * FROM book WHERE id=? FOR UPDATE", Book.class, connection, bookId);
                if (book == null) {
                    throw new ApiException("图书不存在");
                }
                LocalDateTime now = LocalDateTime.now();
                insertCopies(connection, bookId, 1, now);
                JDBCTool.executeUpdate(connection, "UPDATE book SET stock=stock+1,updateTime=? WHERE id=?", now, bookId);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("新增副本失败：" + e.getMessage());
        }
    }

    /** 删除一个副本，库存 -1；已借出的副本不能删除 */
    public void deleteCopy(String copyId) {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String bookId;
                boolean borrowed;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT bookId,isBorrowed FROM bookcopy WHERE id=? FOR UPDATE")) {
                    statement.setString(1, copyId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new ApiException("副本不存在");
                        }
                        bookId = rs.getString("bookId");
                        borrowed = rs.getBoolean("isBorrowed");
                    }
                }
                if (borrowed) {
                    throw new ApiException("该副本正在借阅中，不能删除");
                }
                // 借阅记录外键引用了副本，有历史记录的副本无法物理删除
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM loan WHERE bookCopyId=?")) {
                    statement.setString(1, copyId);
                    try (ResultSet rs = statement.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            throw new ApiException("该副本存在借阅记录，不能删除；如需停用请废弃图书");
                        }
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                JDBCTool.executeUpdate(connection, "DELETE FROM bookcopy WHERE id=?", copyId);
                JDBCTool.executeUpdate(connection,
                        "UPDATE book SET stock=CASE WHEN stock>0 THEN stock-1 ELSE 0 END,updateTime=? WHERE id=?",
                        now, bookId);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("删除副本失败：" + e.getMessage());
        }
    }

    /** 图书类型列表 */
    public List<String> types() {
        List<String> types = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT type,COUNT(*) AS c FROM book WHERE type IS NOT NULL AND type<>'' GROUP BY type ORDER BY c DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                types.add(rs.getString("type"));
            }
        } catch (SQLException e) {
            throw new ApiException("查询图书类型失败：" + e.getMessage());
        }
        return types;
    }

    public static void insertCopies(Connection connection, String bookId, int count, LocalDateTime now) {
        if (count <= 0) {
            return;
        }
        String sql = "INSERT INTO bookcopy (id,createTime,updateTime,bookId,isBorrowed) VALUES (?,?,?,?,0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setObject(2, now);
                statement.setObject(3, now);
                statement.setString(4, bookId);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new ApiException("创建图书副本失败：" + e.getMessage());
        }
    }

    public static int countBorrowedCopies(Connection connection, String bookId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM bookcopy WHERE bookId=? AND isBorrowed=1")) {
            statement.setString(1, bookId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new ApiException("统计借出副本失败：" + e.getMessage());
        }
    }

    public static Map<String, Object> toMap(Book book) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", book.getId());
        map.put("title", book.getTitle());
        map.put("author", book.getAuthor());
        map.put("type", book.getType());
        map.put("publisher", book.getPublisher());
        map.put("days", book.getDays());
        map.put("price", book.getPrice());
        map.put("stock", book.getStock());
        map.put("status", book.getStatus());
        map.put("introduction", book.getIntroduction());
        map.put("createTime", Dates.format(book.getCreateTime()));
        map.put("updateTime", Dates.format(book.getUpdateTime()));
        return map;
    }

    private void appendKeyword(StringBuilder sql, List<Object> params, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR author LIKE ? OR type LIKE ? OR publisher LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
    }

    static LocalDateTime toLocalDateTime(Object value) {
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
