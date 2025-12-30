package com.yzx.crazycodingbytecommon.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @className: BusinessException
 * @author: yzx
 * @date: 2025/12/30 20:59
 * @Version: 1.0
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private Integer code;
    private String message;

    public BusinessException() {
        super();
        this.code = ResultCode.SYSTEM_ERROR.getCode();
        this.message = ResultCode.SYSTEM_ERROR.getMessage();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }
}