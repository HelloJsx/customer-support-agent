package com.support.agent.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST, "请求参数不合法"),
    VALIDATION_ERROR(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "资源不存在"),
    INVALID_STATE(40900, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    APPROVAL_REQUIRED(40901, HttpStatus.CONFLICT, "该操作需要人工审批"),
    AI_UNAVAILABLE(50300, HttpStatus.SERVICE_UNAVAILABLE, "AI 服务暂不可用"),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
