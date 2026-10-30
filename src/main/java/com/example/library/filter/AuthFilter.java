package com.example.library.filter;

import com.example.library.entity.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 登录校验 + 页面角色校验。
 * 未登录访问接口返回 401 JSON，访问页面跳转到登录页。
 */
public class AuthFilter implements Filter {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/login.html", "/register.html", "/captcha", "/css/", "/js/", "/vendor/", "/img/", "/favicon.ico",
            "/api/auth/login", "/api/auth/register", "/api/auth/majors");

    /** 页面访问角色限制 */
    private static final Map<String, List<String>> PAGE_ROLES = Map.of(
            "/users.html", List.of("管理员"),
            "/books.html", List.of("管理员", "操作员"),
            "/loan-query.html", List.of("管理员"),
            "/stats.html", List.of("管理员"),
            "/review.html", List.of("管理员"),
            "/recommend.html", List.of("教师"),
            "/borrow.html", List.of("管理员", "操作员", "学生", "教师"),
            "/return.html", List.of("管理员", "操作员", "学生", "教师"),
            "/my-loans.html", List.of("管理员", "操作员", "学生", "教师"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path) || "/".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            if (path.startsWith("/api/")) {
                writeUnauthorized(resp);
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.html");
            }
            return;
        }

        List<String> roles = PAGE_ROLES.get(path);
        if (roles != null && !roles.contains(user.getRole())) {
            if (path.startsWith("/api/")) {
                writeForbidden(resp);
            } else {
                resp.sendRedirect(req.getContextPath() + "/index.html");
            }
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || (prefix.endsWith("/") && path.startsWith(prefix))) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":401,\"msg\":\"登录已过期，请重新登录\",\"data\":null}");
    }

    private void writeForbidden(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":403,\"msg\":\"没有访问权限\",\"data\":null}");
    }
}
