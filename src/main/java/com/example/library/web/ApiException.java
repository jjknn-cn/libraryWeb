package com.example.library.web;

/**
 * 业务异常：由 Servlet 统一捕获并转换为 JSON 错误信息。
 */
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(String message) {
        this(1, message);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
