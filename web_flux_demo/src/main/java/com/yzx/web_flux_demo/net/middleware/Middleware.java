package com.yzx.web_flux_demo.net.middleware;

import com.yzx.web_flux_demo.net.config.Http2ResponseCallback;
import com.yzx.web_flux_demo.net.config.core.Context;
import com.yzx.web_flux_demo.net.config.core.Request;

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
