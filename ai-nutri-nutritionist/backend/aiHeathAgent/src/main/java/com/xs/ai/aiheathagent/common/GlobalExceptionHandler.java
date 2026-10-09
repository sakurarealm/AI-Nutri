package com.xs.ai.aiheathagent.common;



import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.springframework.web.server.ResponseStatusException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizEx.class)
    public ApiResp<?> biz(BizEx e){ return ApiResp.fail(e.getCode(), e.getMessage()); }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResp<?> bad(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getAllErrors().stream().findFirst().map(x->x.getDefaultMessage()).orElse("参数错误");
        return ApiResp.fail(ErrorCode.PARAM_INVALID, msg);
    }


    @ExceptionHandler(DuplicateKeyException.class)
    public ApiResp<?> dup(DuplicateKeyException e){ return ApiResp.fail(ErrorCode.BIZ_ERROR, "账号已存在"); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResp<?> integrity(DataIntegrityViolationException e){
        String raw = "";
        try {
            raw = e.getMostSpecificCause() == null ? "" : String.valueOf(e.getMostSpecificCause().getMessage());
        } catch (Exception ignored) {
            raw = "";
        }
        String msg = "数据关联导致操作失败";
        String lower = raw == null ? "" : raw.toLowerCase();
        if (lower.contains("conversation_records") || lower.contains("fk_cr_user")) {
            msg = "存在对话记录关联，无法删除用户";
        } else if (lower.contains("health_records") || lower.contains("fk_hr_user")) {
            msg = "存在健康记录关联，无法删除用户";
        } else if (lower.contains("login_records") || lower.contains("fk_lr_user")) {
            msg = "存在登录记录关联，无法删除用户";
        }
        return ApiResp.fail(ErrorCode.BIZ_ERROR, msg);
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResp<?> status(ResponseStatusException e){
        HttpStatus st = e.getStatusCode() instanceof HttpStatus hs ? hs : HttpStatus.INTERNAL_SERVER_ERROR;
        String msg = e.getReason() == null ? "" : e.getReason();
        if (msg.isBlank()) msg = "请求失败";

        int code = switch (st) {
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case BAD_REQUEST -> ErrorCode.PARAM_INVALID;
            default -> (st.is4xxClientError() ? ErrorCode.BIZ_ERROR : ErrorCode.SERVER_ERROR);
        };
        return ApiResp.fail(code, msg);
    }

    @ExceptionHandler(TencentCloudSDKException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResp<?> tencent(TencentCloudSDKException e){
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("FailedOperation.UserNotRegistered") || msg.contains("User is unopened")) {
            return ApiResp.fail(ErrorCode.BIZ_ERROR, "腾讯云语音识别未开通，请在腾讯云控制台开通 ASR（一句话识别）");
        }
        if (msg.contains("FailedOperation.ErrorRecognize") && msg.contains("audio data empty")) {
            return ApiResp.fail(ErrorCode.BIZ_ERROR, "音频为空或格式不兼容，请按住说话 1 秒以上，并优先用 wav 录音");
        }
        if (msg.contains("InvalidParameterValue.ErrorInvalidVoicedata") || msg.contains("Audio decoding failed")) {
            return ApiResp.fail(ErrorCode.BIZ_ERROR, "音频格式解码失败，请改用 pcm 录音（后端会自动转 wav）");
        }
        return ApiResp.fail(ErrorCode.BIZ_ERROR, "腾讯云语音服务异常：" + msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResp<?> ex(Exception e){ e.printStackTrace(); return ApiResp.fail(ErrorCode.SERVER_ERROR, "系统繁忙"); }
}
