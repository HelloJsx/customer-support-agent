package com.support.agent.common.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message);
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.INVALID_STATE, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
