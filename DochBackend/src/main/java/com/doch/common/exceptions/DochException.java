package com.doch.common.exceptions;

public class DochException extends RuntimeException {
    private final DochErrorCode errorCode;

    public DochException(DochErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public DochErrorCode getErrorCode() {
        return errorCode;
    }
}
