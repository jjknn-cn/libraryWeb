package com.example.library.servlet;

import com.example.library.dao.LoanDao;
import com.example.library.entity.User;
import com.example.library.web.ApiException;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 借书、还书、借阅记录查询、借阅数据统计。
 */
@WebServlet("/api/loan/*")
public class LoanServlet extends BaseServlet {

    private final LoanDao loanDao = new LoanDao();

    @Override
    protected void handle(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws Exception {
        switch (path) {
            case "/borrow" -> borrow(req, resp);
            case "/return" -> giveBack(req, resp);
            case "/mine" -> mine(req, resp);
            case "/query" -> query(req, resp);
            case "/active" -> active(req, resp);
            case "/statsByType" -> ok(resp, loanDao.statsByType());
            case "/statsByMajor" -> ok(resp, loanDao.statsByMajor());
            default -> throw new ApiException(404, "接口不存在：" + path);
        }
    }

    private void borrow(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        JsonObject body = body(req);
        String bookId = requiredStr(body, "bookId", "请选择要借阅的图书");
        ok(resp, loanDao.borrow(user.getId(), bookId));
    }

    private void giveBack(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = currentUser(req);
        JsonObject body = body(req);
        String loanId = requiredStr(body, "loanId", "请选择要归还的借阅记录");
        loanDao.returnLoan(loanId, user);
        ok(resp, null);
    }

    private void mine(HttpServletRequest req, HttpServletResponse resp) {
        User user = currentUser(req);
        ok(resp, loanDao.mine(user.getId(), param(req, "status"), param(req, "start"), param(req, "end")));
    }

    private void query(HttpServletRequest req, HttpServletResponse resp) {
        requireRole(req, "管理员");
        boolean overdue = "1".equals(param(req, "overdue")) || "true".equalsIgnoreCase(param(req, "overdue"));
        ok(resp, loanDao.query(param(req, "keyword"), overdue, param(req, "start"), param(req, "end")));
    }

    private void active(HttpServletRequest req, HttpServletResponse resp) {
        requireRole(req, "管理员", "操作员");
        ok(resp, loanDao.active());
    }
}
