package com.example.library.servlet;

import com.example.library.entity.User;
import com.example.library.util.Dates;
import com.example.library.web.ApiException;
import com.example.library.web.Result;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Servlet 基类：统一分发 GET/POST、解析 JSON、输出 JSON、处理异常。
 * 子类通过 pathInfo 区分具体操作，例如 /api/book/list、/api/book/add。
 */
public abstract class BaseServlet extends HttpServlet {

    protected static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) -> new JsonPrimitive(Dates.format(src)))
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (src, type, ctx) -> new JsonPrimitive(Dates.format(src)))
            .create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    private void dispatch(HttpServletRequest req, HttpServletResponse resp) {
        resp.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        try {
            handle(req, resp, req.getMethod(), path);
        } catch (ApiException e) {
            if (e.getCode() == 401) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
            writeJson(resp, Result.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log("请求处理失败：" + req.getRequestURI(), e);
            writeJson(resp, Result.fail(1, "服务器异常：" + e.getMessage()));
        }
    }

    /** 子类实现具体业务 */
    protected abstract void handle(HttpServletRequest req, HttpServletResponse resp, String method, String path)
            throws Exception;

    protected String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? null : value.trim();
    }

    protected String requiredParam(HttpServletRequest req, String name, String message) {
        String value = param(req, name);
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value;
    }

    /** 读取请求体：既支持 application/json，也兼容表单提交 */
    protected JsonObject body(HttpServletRequest req) throws IOException {
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
            // 表单请求必须交给容器解析参数，不能先把请求体读掉
            return fromParameters(req);
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        String raw = text.toString().trim();
        if (raw.startsWith("{")) {
            try {
                JsonElement element = JsonParser.parseString(raw);
                if (element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            } catch (RuntimeException e) {
                throw new ApiException("请求体不是合法的 JSON");
            }
            return new JsonObject();
        }
        return fromParameters(req);
    }

    private JsonObject fromParameters(HttpServletRequest req) {
        JsonObject object = new JsonObject();
        for (java.util.Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0 && values[0] != null) {
                object.addProperty(entry.getKey(), values[0]);
            }
        }
        return object;
    }

    protected String str(JsonObject body, String key) {
        JsonElement element = body.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        String value = element.getAsString();
        return value == null ? null : value.trim();
    }

    protected String requiredStr(JsonObject body, String key, String message) {
        String value = str(body, key);
        if (value == null || value.isBlank()) {
            throw new ApiException(message);
        }
        return value;
    }

    protected Integer intValue(JsonObject body, String key) {
        String value = str(body, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ApiException("数字格式不正确：" + key);
        }
    }

    protected Double doubleValue(JsonObject body, String key) {
        String value = str(body, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ApiException("数字格式不正确：" + key);
        }
    }

    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute("user");
        if (!(user instanceof User)) {
            throw new ApiException(401, "登录已失效，请重新登录");
        }
        return (User) user;
    }

    protected User requireRole(HttpServletRequest req, String... roles) {
        User user = currentUser(req);
        for (String role : roles) {
            if (role.equals(user.getRole())) {
                return user;
            }
        }
        throw new ApiException(403, "当前角色没有操作权限");
    }

    protected void ok(HttpServletResponse resp, Object data) {
        writeJson(resp, Result.ok(data));
    }

    protected void fail(HttpServletResponse resp, String message) {
        writeJson(resp, Result.fail(message));
    }

    protected void writeJson(HttpServletResponse resp, Object data) {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            PrintWriter writer = resp.getWriter();
            writer.write(GSON.toJson(data));
            writer.flush();
        } catch (IOException e) {
            log("写出 JSON 失败", e);
        }
    }
}
