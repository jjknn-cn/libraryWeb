package com.example.library.dao;

import com.example.library.entity.Book;
import com.example.library.entity.BookRecommend;
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
 * 教师推荐图书 / 管理员审阅推荐的数据访问。
 */
public class RecommendDao {

    /** 馆内是否已有相关书籍（按书名模糊匹配） */
    public List<Book> findExisting(String bookName) {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM book WHERE title LIKE ? AND status='正常' ORDER BY title ASC")) {
            statement.setString(1, "%" + (bookName == null ? "" : bookName.trim()) + "%");
            try (ResultSet rs = statement.executeQuery()) {
                return JDBCTool.resultSetToList(rs, Book.class);
            }
        } catch (SQLException e) {
            throw new ApiException("查询馆内图书失败：" + e.getMessage());
        }
    }

    public void submit(BookRecommend recommend) {
        LocalDateTime now = LocalDateTime.now();
        recommend.setId(UUID.randomUUID().toString());
        recommend.setCreateTime(now);
        recommend.setUpdateTime(now);
        recommend.setStatus("待审核");
        recommend.setReason(null);
        try (Connection connection = DBUtil.getConnection()) {
            JDBCTool.save(recommend, connection);
        } catch (SQLException e) {
            throw new ApiException("提交推荐失败：" + e.getMessage());
        }
    }

    /** 推荐列表（管理员审阅） */
    public List<Map<String, Object>> list(String status) {
        String sql = "SELECT r.id,r.bookName,r.author,r.publisher,r.price,r.status,r.reason,r.createTime,"
                + "u.name AS userName,u.role AS userRole FROM bookrecommend r "
                + "JOIN user u ON u.id=r.userId";
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql += " WHERE r.status=?";
            params.add(status);
        }
        sql += " ORDER BY r.createTime DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", rs.getString("id"));
                    map.put("bookName", rs.getString("bookName"));
                    map.put("author", rs.getString("author"));
                    map.put("publisher", rs.getString("publisher"));
                    map.put("price", rs.getObject("price"));
                    map.put("status", rs.getString("status"));
                    map.put("reason", rs.getString("reason"));
                    map.put("userName", rs.getString("userName"));
                    map.put("userRole", rs.getString("userRole"));
                    map.put("createTime", Dates.format(toLocalDateTime(rs.getObject("createTime"))));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            throw new ApiException("查询推荐失败：" + e.getMessage());
        }
        return list;
    }

    /** 审阅通过：把推荐图书加入图书列表（创建图书 + 副本），并更新推荐状态 */
    public void approve(String id, String type, Integer days, Integer stock, String introduction) {
        if (type == null || type.isBlank()) {
            throw new ApiException("请填写图书类型");
        }
        if (days == null || days <= 0) {
            throw new ApiException("还书期限天数必须大于 0");
        }
        if (stock == null || stock < 0) {
            throw new ApiException("库存数量不能为负数");
        }
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BookRecommend recommend = JDBCTool.queryOne(
                        "SELECT * FROM bookrecommend WHERE id=? FOR UPDATE", BookRecommend.class, connection, id);
                if (recommend == null) {
                    throw new ApiException("推荐记录不存在");
                }
                if (!"待审核".equals(recommend.getStatus())) {
                    throw new ApiException("该推荐已经处理过");
                }
                LocalDateTime now = LocalDateTime.now();
                Book book = new Book();
                book.setId(UUID.randomUUID().toString());
                book.setCreateTime(now);
                book.setUpdateTime(now);
                book.setTitle(recommend.getBookName());
                book.setAuthor(recommend.getAuthor());
                book.setPublisher(recommend.getPublisher());
                book.setPrice(recommend.getPrice() == null ? 0D : recommend.getPrice());
                book.setType(type);
                book.setDays(days);
                book.setStock(stock);
                book.setStatus("正常");
                book.setIntroduction(introduction);
                JDBCTool.save(book, connection);
                BookDao.insertCopies(connection, book.getId(), stock, now);
                JDBCTool.executeUpdate(connection,
                        "UPDATE bookrecommend SET status='已通过',updateTime=? WHERE id=?", now, id);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("审阅通过失败：" + e.getMessage());
        }
    }

    /** 驳回推荐：必须填写理由 */
    public void reject(String id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException("请填写驳回理由");
        }
        try (Connection connection = DBUtil.getConnection()) {
            int rows = JDBCTool.executeUpdate(connection,
                    "UPDATE bookrecommend SET status='已驳回',reason=?,updateTime=? WHERE id=? AND status='待审核'",
                    reason.trim(), LocalDateTime.now(), id);
            if (rows == 0) {
                throw new ApiException("推荐记录不存在或已处理");
            }
        } catch (SQLException e) {
            throw new ApiException("驳回推荐失败：" + e.getMessage());
        }
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
