package com.doch.common.exceptions;

public class AuthenticationException extends DochException {
    public AuthenticationException(DochErrorCode errorCode) {
        super(errorCode);
    }
}
