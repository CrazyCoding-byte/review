package com.yzx.web_flux_demo.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: SimpleJson
 * @author: yzx
 * @date: 2025/11/22 16:17
 * @Version: 1.0
 * @description:
 */
public class SimpleJson {
    // --- 序列化 (Java Object -> JSON String) ---
    public static String toJSON(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) return mapToJSON((Map<?, ?>) obj);
        if (obj instanceof List) return listToJSON((List<?>) obj);
        // 如果是POJO，则通过反射处理
        return pojoToJSON(obj);
    }

    private static String mapToJSON(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append("\"").append(escape(entry.getKey().toString())).append("\":");
            sb.append(toJSON(entry.getValue())).append(",");
        }
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        return sb.append("}").toString();
    }

    private static String listToJSON(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Object item : list) {
            sb.append(toJSON(item)).append(",");
        }
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        return sb.append("]").toString();
    }

    private static String pojoToJSON(Object pojo) {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = pojo.getClass();
        // 获取所有字段，包括私有字段
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true); // 允许访问私有字段
                Object value = field.get(pojo);
                map.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                // 忽略无法访问的字段
            }
        }
        return mapToJSON(map);
    }

    // --- 反序列化 (JSON String -> Java Object) ---
    // 注意：这是一个简化版本的反序列化，只处理 {} 和 [] 结构
    public static <T> T fromJSON(String json, Class<T> clazz) {
        json = json.trim();
        if (json.equals("null")) return null;

        try {
            if (clazz == Map.class) {
                // 简化处理，实际需要更复杂的解析器
                // 这里我们假设使用一个虚构的 Map 解析器
                return (T) parseMap(json);
            } else if (clazz == List.class) {
                // 同理，假设一个虚构的 List 解析器
                return (T) parseList(json);
            } else {
                // 如果是POJO，先解析成Map，再注入到对象中
                Map<String, Object> map = parseMap(json);
                return mapToPojo(map, clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    // 以下是非常简化的解析逻辑，仅用于演示
    // 一个健壮的JSON解析器需要处理各种边缘情况，代码会复杂得多
    private static Map<String, Object> parseMap(String json) {
        Map<String, Object> map = new HashMap<>();
        // 移除首尾的 {}
        String content = json.substring(1, json.length() - 1);
        String[] keyValuePairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // 按逗号分割，忽略引号内的逗号

        for (String pair : keyValuePairs) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) continue;

            String key = parts[0].trim().replace("\"", "");
            String valueStr = parts[1].trim();

            Object value;
            if (valueStr.startsWith("\"")) {
                value = valueStr.substring(1, valueStr.length() - 1);
            } else if (valueStr.equals("true") || valueStr.equals("false")) {
                value = Boolean.parseBoolean(valueStr);
            } else if (valueStr.matches("-?\\d+")) {
                value = Long.parseLong(valueStr);
            } else if (valueStr.matches("-?\\d+\\.\\d+")) {
                value = Double.parseDouble(valueStr);
            } else if (valueStr.startsWith("{")) {
                value = parseMap(valueStr);
            } else if (valueStr.startsWith("[")) {
                value = parseList(valueStr);
            } else {
                value = valueStr; // 默认为字符串
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> parseList(String json) {
        List<Object> list = new ArrayList<>();
        // 移除首尾的 []
        String content = json.substring(1, json.length() - 1);
        if (content.isEmpty()) return list;
        String[] elements = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String elem : elements) {
            String trimmedElem = elem.trim();
            if (trimmedElem.startsWith("{")) {
                list.add(parseMap(trimmedElem));
            } else if (trimmedElem.startsWith("[")) {
                list.add(parseList(trimmedElem));
            } else {
                // 简单地将非对象、非数组的值视为字符串
                list.add(trimmedElem.replace("\"", ""));
            }
        }
        return list;
    }

    private static <T> T mapToPojo(Map<String, Object> map, Class<T> clazz) throws Exception {
        T obj = clazz.getDeclaredConstructor().newInstance(); // 要求POJO有一个无参构造函数

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                Field field = clazz.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                // 这里简化了类型转换，实际需要更复杂的逻辑来匹配字段类型
                field.set(obj, entry.getValue());
            } catch (NoSuchFieldException e) {
                // 忽略POJO中不存在的字段
            }
        }
        return obj;
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
