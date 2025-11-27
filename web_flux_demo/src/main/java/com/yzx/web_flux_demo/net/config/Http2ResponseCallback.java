package com.yzx.web_flux_demo.net.config;

import com.yzx.web_flux_demo.net.config.core.Http2Response;

/**
 * @className: Http2ResponseCallback
 * @author: yzx
 * @date: 2025/11/22 17:09
 * @Version: 1.0
 * @description:
 */
public interface Http2ResponseCallback {
    void onSuccess(Http2Response response);
    void onFailure(Throwable cause);
}
