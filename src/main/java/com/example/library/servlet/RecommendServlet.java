package com.example.library.servlet;

import com.example.library.dao.BookDao;
import com.example.library.dao.RecommendDao;
import com.example.library.entity.Book;
import com.example.library.entity.BookRecommend;
import com.example.library.entity.User;
import com.example.library.web.ApiException;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师推荐图书 / 管理员审阅推荐图书。
 */
@WebServlet("/api/recommend/*")
public class RecommendServlet extends BaseServlet {

    private final RecommendDao recommendDao = new RecommendDao();

    @Override
    protected void handle(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws Exception {
        switch (path) {
            case "/submit" -> submit(req, resp);
            case "/list" -> list(req, resp);
            case "/approve" -> approve(req, resp);
            case "/reject" -> reject(req, resp);
            default -> throw new ApiException(404, "接口不存在：" + path);
        }
    }

    /** 推荐图书：馆内已有相关书籍则直接返回图书信息，否则提交审核 */
    private void submit(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = requireRole(req, "教师");
        JsonObject body = body(req);
        String bookName = limit(requiredStr(body, "bookName", "请输入推荐书名"), 20, "书名不能超过 20 个字符");
        String author = limit(requiredStr(body, "author", "请输入作者"), 20, "作者不能超过 20 个字符");
        String publisher = limit(requiredStr(body, "publisher", "请输入出版社"), 25, "出版社不能超过 25 个字符");
        Double price = doubleValue(body, "price");
        if (price == null || price < 0) {
            throw new ApiException("价格不能为空且不能为负数");
        }

        List<Book> existing = recommendDao.findExisting(bookName);
        Map<String, Object> data = new LinkedHashMap<>();
        if (!existing.isEmpty()) {
            data.put("exists", true);
            data.put("message", "馆内已有相关书籍，无需重复推荐");
            data.put("books", existing.stream().map(BookDao::toMap).toList());
            ok(resp, data);
            return;
        }

        BookRecommend recommend = new BookRecommend();
        recommend.setUserId(user.getId());
        recommend.setBookName(bookName);
        recommend.setAuthor(author);
        recommend.setPublisher(publisher);
        recommend.setPrice(price);
        recommendDao.submit(recommend);
        data.put("exists", false);
        data.put("message", "馆内暂无该书，已自动提交图书审核");
        data.put("id", recommend.getId());
        ok(resp, data);
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员");
        ok(resp, recommendDao.list(param(req, "status")));
    }

    /** 审阅通过：补全图书信息后加入图书列表 */
    private void approve(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员");
        JsonObject body = body(req);
        String id = requiredStr(body, "id", "缺少推荐记录 id");
        String type = limit(requiredStr(body, "type", "请选择图书类型"), 10, "类型不能超过 10 个字符");
        Integer days = intValue(body, "days");
        if (days == null || days <= 0 || days > 365) {
            throw new ApiException("还书期限天数应为 1-365 的整数");
        }
        Integer stock = intValue(body, "stock");
        if (stock == null || stock < 0 || stock > 1000) {
            throw new ApiException("库存应为 0-1000 的整数");
        }
        String introduction = str(body, "introduction");
        if (introduction != null && introduction.trim().length() > 512) {
            throw new ApiException("图书简介不能超过 512 个字符");
        }
        recommendDao.approve(id, type, days, stock, introduction == null ? null : introduction.trim());
        ok(resp, null);
    }

    private void reject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员");
        JsonObject body = body(req);
        String id = requiredStr(body, "id", "缺少推荐记录 id");
        String reason = limit(requiredStr(body, "reason", "请填写驳回理由"), 100, "驳回理由不能超过 100 个字符");
        recommendDao.reject(id, reason);
        ok(resp, null);
    }

    private String limit(String value, int max, String message) {
        if (value.length() > max) {
            throw new ApiException(message);
        }
        return value;
    }
}
