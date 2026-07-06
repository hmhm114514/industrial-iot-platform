package com.practice.common;

public class ApiResponse<T> {
    public int code;
    public String message;
    public T data;
    public long timestamp;
    public ApiResponse() {}
    public ApiResponse(int code, String message, T data) { this.code = code; this.message = message; this.data = data; this.timestamp = System.currentTimeMillis(); }
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(200, "success", data); }
    public static <T> ApiResponse<T> fail(String message) { return new ApiResponse<>(500, message, null); }
}
