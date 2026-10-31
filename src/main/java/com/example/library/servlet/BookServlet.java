package com.example.library.servlet;

import com.example.library.dao.BookDao;
import com.example.library.entity.Book;
import com.example.library.web.ApiException;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图书功能：查询、添加、编辑、废弃、副本管理。
 */
@WebServlet("/api/book/*")
public class BookServlet extends BaseServlet {

    private final BookDao bookDao = new BookDao();

    @Override
    protected void handle(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws Exception {
        switch (path) {
            case "/list" -> list(req, resp);
            case "/available" -> available(req, resp);
            case "/get" -> get(req, resp);
            case "/copies" -> copies(req, resp);
            case "/types" -> ok(resp, bookDao.types());
            case "/add" -> add(req, resp);
            case "/update" -> update(req, resp);
            case "/discard" -> discard(req, resp);
            case "/addCopy" -> addCopy(req, resp);
            case "/deleteCopy" -> deleteCopy(req, resp);
            default -> throw new ApiException(404, "接口不存在：" + path);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) {
        currentUser(req);
        ok(resp, toMaps(bookDao.list(param(req, "keyword"))));
    }

    private void available(HttpServletRequest req, HttpServletResponse resp) {
        currentUser(req);
        ok(resp, toMaps(bookDao.available(param(req, "keyword"))));
    }

    private void get(HttpServletRequest req, HttpServletResponse resp) {
        currentUser(req);
        Book book = bookDao.findById(requiredParam(req, "id", "缺少图书 id"));
        if (book == null) {
            throw new ApiException("图书不存在");
        }
        ok(resp, BookDao.toMap(book));
    }

    private void copies(HttpServletRequest req, HttpServletResponse resp) {
        currentUser(req);
        String bookId = requiredParam(req, "bookId", "缺少图书 id");
        Book book = bookDao.findById(bookId);
        if (book == null) {
            throw new ApiException("图书不存在");
        }
        Map<String, Object> data = BookDao.toMap(book);
        data.put("copies", bookDao.copies(bookId));
        ok(resp, data);
    }

    private void add(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员", "操作员");
        JsonObject body = body(req);
        Book book = readBook(body, true);
        bookDao.add(book);
        ok(resp, BookDao.toMap(book));
    }

    private void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员", "操作员");
        JsonObject body = body(req);
        String id = requiredStr(body, "id", "缺少图书 id");
        if (bookDao.findById(id) == null) {
            throw new ApiException("图书不存在");
        }
        Book book = readBook(body, false);
        book.setId(id);
        bookDao.updateInfo(book);
        ok(resp, null);
    }

    private void discard(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员", "操作员");
        JsonObject body = body(req);
        bookDao.discard(requiredStr(body, "id", "缺少图书 id"));
        ok(resp, null);
    }

    private void addCopy(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员", "操作员");
        JsonObject body = body(req);
        bookDao.addCopy(requiredStr(body, "bookId", "缺少图书 id"));
        ok(resp, null);
    }

    private void deleteCopy(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        requireRole(req, "管理员", "操作员");
        JsonObject body = body(req);
        bookDao.deleteCopy(requiredStr(body, "copyId", "缺少副本 id"));
        ok(resp, null);
    }

    private Book readBook(JsonObject body, boolean withStock) {
        Book book = new Book();
        book.setTitle(limit(requiredStr(body, "title", "请输入书名"), 30, "书名不能超过 30 个字符"));
        book.setAuthor(limit(requiredStr(body, "author", "请输入作者"), 30, "作者不能超过 30 个字符"));
        book.setType(limit(requiredStr(body, "type", "请选择图书类型"), 10, "类型不能超过 10 个字符"));
        book.setPublisher(limit(requiredStr(body, "publisher", "请输入出版社"), 20, "出版社不能超过 20 个字符"));
        Integer days = intValue(body, "days");
        if (days == null || days <= 0 || days > 365) {
            throw new ApiException("还书期限天数应为 1-365 的整数");
        }
        book.setDays(days);
        Double price = doubleValue(body, "price");
        if (price == null || price < 0) {
            throw new ApiException("价格不能为空且不能为负数");
        }
        book.setPrice(price);
        String introduction = str(body, "introduction");
        if (introduction != null) {
            introduction = introduction.trim();
            if (introduction.length() > 512) {
                throw new ApiException("图书简介不能超过 512 个字符");
            }
        }
        book.setIntroduction(introduction);
        if (withStock) {
            Integer stock = intValue(body, "stock");
            if (stock == null || stock < 0 || stock > 1000) {
                throw new ApiException("库存应为 0-1000 的整数（系统会按库存自动创建副本）");
            }
            book.setStock(stock);
            book.setStatus("正常");
        }
        return book;
    }

    private String limit(String value, int max, String message) {
        if (value.length() > max) {
            throw new ApiException(message);
        }
        return value;
    }

    private List<Map<String, Object>> toMaps(List<Book> books) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Book book : books) {
            list.add(BookDao.toMap(book));
        }
        return list;
    }
}
