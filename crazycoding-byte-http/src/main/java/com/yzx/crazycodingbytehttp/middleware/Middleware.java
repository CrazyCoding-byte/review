package com.yzx.crazycodingbytehttp.middleware;


import com.yzx.crazycodingbytehttp.config.core.Context;

/**
 * @className: Middleware
 * @author: yzx
 * @date: 2025/11/23 2:00
 * @Version: 1.0
 * @description:
 */
public interface Middleware {
    void handle(Context context) throws Exception;
}
