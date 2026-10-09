package com.xs.ai.aiheathagent.common;


import lombok.*;


@Data @AllArgsConstructor @NoArgsConstructor
public class ApiResp<T> {
    private int code; // 0 success
    private String message;
    private T data;
    public static <T> ApiResp<T> ok(T data){ return new ApiResp<>(0, "ok", data);}
    public static <T> ApiResp<T> fail(int code, String msg){ return new ApiResp<>(code, msg, null);}
}
