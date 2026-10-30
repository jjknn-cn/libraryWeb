package com.example.library.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一的 JSON 返回结构：code=0 成功，code=1 业务失败。
 */
public class Result {

    public static Map<String, Object> ok() {
        return ok(null);
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", 0);
        map.put("msg", "ok");
        map.put("data", data);
        return map;
    }

    public static Map<String, Object> fail(String message) {
        return fail(1, message);
    }

    public static Map<String, Object> fail(int code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("msg", message);
        map.put("data", null);
        return map;
    }
}
