package com.yzx.crazycodingbytegateway.config;

import lombok.Data;

import java.util.List;

/**
 * @className: Route
 * @author: yzx
 * @date: 2025/11/14 1:21
 * @Version: 1.0
 * @description:
 */
@Data
public class Route {
    private String id;
    private String uri;
    private MatchType matchType;
    private String matchKey;
    private GrayRule grayRule;
    private RouteConfig routeConfig;
    private int priority;

    public enum MatchType {
        private final String desc;
        private final String example;

        MatchType(String desc, String example) {
            this.desc = desc;
            this.example = example;
        }
        public String
    }
}
