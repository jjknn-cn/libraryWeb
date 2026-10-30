package com.example.library.servlet;

import com.example.library.dao.UserDao;
import com.example.library.entity.Admin;
import com.example.library.entity.Operator;
import com.example.library.entity.Student;
import com.example.library.entity.Teacher;
import com.example.library.entity.User;
import com.example.library.web.ApiException;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 认证相关：登录、注册、当前用户、退出。
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseServlet {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Set<String> ROLES = Set.of("管理员", "操作员", "学生", "教师");

    private final UserDao userDao = new UserDao();

    @Override
    protected void handle(HttpServletRequest req, HttpServletResponse resp, String method, String path) throws Exception {
        switch (path) {
            case "/login" -> login(req, resp);
            case "/register" -> register(req, resp);
            case "/current" -> current(req, resp);
            case "/logout" -> logout(req, resp);
            case "/majors" -> ok(resp, userDao.listMajors());
            default -> throw new ApiException(404, "接口不存在：" + path);
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = body(req);
        String name = requiredStr(body, "name", "请输入用户名");
        String password = requiredStr(body, "password", "请输入密码");
        String captcha = requiredStr(body, "captcha", "请输入验证码");

        HttpSession session = req.getSession(true);
        Object expected = session.getAttribute("captcha");
        session.removeAttribute("captcha");
        if (expected == null || !String.valueOf(expected).equalsIgnoreCase(captcha)) {
            throw new ApiException("验证码错误或已过期，请刷新验证码");
        }

        User user = userDao.findByName(name);
        if (user == null || !password.equals(user.getPassword())) {
            throw new ApiException("用户名或密码错误");
        }
        userDao.updateLoginTime(user.getId());
        user.setLoginTime(LocalDateTime.now());
        session.setAttribute("user", user);

        Map<String, Object> data = UserDao.toMap(user);
        data.put("permissions", user.hasPermission());
        ok(resp, data);
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = body(req);
        String name = requiredStr(body, "name", "请输入姓名");
        if (name.length() < 2 || name.length() > 20) {
            throw new ApiException("姓名长度应为 2-20 个字符");
        }
        String password = requiredStr(body, "password", "请输入密码");
        if (password.length() < 6) {
            throw new ApiException("密码长度不能少于 6 位");
        }
        String confirm = str(body, "confirm");
        if (confirm == null || !password.equals(confirm)) {
            throw new ApiException("两次输入的密码不一致");
        }
        String phone = requiredStr(body, "phone", "请输入手机号");
        if (!PHONE.matcher(phone).matches()) {
            throw new ApiException("手机号格式不正确");
        }
        String role = requiredStr(body, "role", "请选择角色");
        if (!ROLES.contains(role)) {
            throw new ApiException("请选择正确的角色");
        }
        String major = requiredStr(body, "major", "请选择专业");

        if (userDao.existsName(name)) {
            throw new ApiException("该姓名已被注册，请更换姓名");
        }
        if (userDao.existsPhone(phone)) {
            throw new ApiException("该手机号已被注册");
        }

        User user = createUser(role);
        LocalDateTime now = LocalDateTime.now();
        user.setId(UUID.randomUUID().toString());
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setName(name);
        user.setPassword(password);
        user.setPhone(phone);
        user.setRole(role);
        user.setMajor(major);

        if (user instanceof Student student) {
            student.setGrade(requiredStr(body, "grade", "请输入年级"));
            student.setCollege(requiredStr(body, "college", "请输入所属学院"));
        } else if (user instanceof Teacher teacher) {
            teacher.setCollege(requiredStr(body, "college", "请输入所属学院"));
            teacher.setTitle(requiredStr(body, "title", "请输入职称"));
        } else if (user instanceof Admin admin) {
            Integer level = intValue(body, "permissionLevel");
            if (level == null || level < 1 || level > 9) {
                throw new ApiException("权限等级应为 1-9 的整数");
            }
            admin.setPermissionLevel(level);
            String entryTime = requiredStr(body, "entryTime", "请选择入职时间");
            try {
                admin.setEntryTime(LocalDate.parse(entryTime));
            } catch (Exception e) {
                throw new ApiException("入职时间格式应为 yyyy-MM-dd");
            }
        } else if (user instanceof Operator operator) {
            operator.setArea(requiredStr(body, "area", "请输入负责区域"));
            operator.setDuty(requiredStr(body, "duty", "请输入职务"));
        }

        userDao.insert(user);
        ok(resp, UserDao.toMap(user));
    }

    private void current(HttpServletRequest req, HttpServletResponse resp) {
        User user = currentUser(req);
        Map<String, Object> data = UserDao.toMap(user);
        data.put("permissions", user.hasPermission());
        ok(resp, data);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ok(resp, null);
    }

    private User createUser(String role) {
        return switch (role) {
            case "管理员" -> new Admin();
            case "操作员" -> new Operator();
            case "教师" -> new Teacher();
            default -> new Student();
        };
    }
}
