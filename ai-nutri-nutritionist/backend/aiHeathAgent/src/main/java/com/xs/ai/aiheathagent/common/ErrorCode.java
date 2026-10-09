package com.xs.ai.aiheathagent.common;


public interface ErrorCode {
    int PARAM_INVALID = 1001;
    int UNAUTHORIZED = 1002;
    int FORBIDDEN = 1003;
    int BIZ_ERROR = 2001;
    int TOO_MANY_REQUESTS = 2002;
    int SERVER_ERROR = 5000;
}