package com.yzx.crazycodingbytemq.util;

/**
 * @className: StringUtils
 * @author: yzx
 * @date: 2025/11/14 15:29
 * @Version: 1.0
 * @description:
 */
public class StringUtils {

    /**
     * 判断字符串是否为空（null或空串）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否非空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
