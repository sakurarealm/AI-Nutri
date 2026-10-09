package com.xs.ai.aiheathagent.common;


public class BizEx extends RuntimeException {
    private final int code;
    public BizEx(int code, String msg){ super(msg); this.code = code; }
    public int getCode(){ return code; }
}