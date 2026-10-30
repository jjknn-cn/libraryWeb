package com.example.library.servlet;

import com.example.library.dao.UserDao;
import com.example.library.entity.User;
import com.example.library.web.ApiException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户信息查询（仅管理员，只读）。
 */
@WebServlet("/api/user/*")
public class UserServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void handle(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws Exception {
        switch (path) {
            case "/list" -> list(req, resp);
            case "/get" -> get(req, resp);
            default -> throw new ApiException(404, "接口不存在：" + path);
        }
    }

    /** 关键字模糊查询，未输入关键字时返回空列表（页面默认不显示数据） */
    private void list(HttpServletRequest req, HttpServletResponse resp) {
        requireRole(req, "管理员");
        String keyword = param(req, "keyword");
        if (keyword == null || keyword.isBlank()) {
            ok(resp, new ArrayList<>());
            return;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (User user : userDao.search(keyword)) {
            list.add(UserDao.toMap(user));
        }
        ok(resp, list);
    }

    private void get(HttpServletRequest req, HttpServletResponse resp) {
        requireRole(req, "管理员");
        String id = requiredParam(req, "id", "缺少用户 id");
        User user = userDao.findById(id);
        if (user == null) {
            throw new ApiException("用户不存在");
        }
        ok(resp, UserDao.toMap(user));
    }
}
