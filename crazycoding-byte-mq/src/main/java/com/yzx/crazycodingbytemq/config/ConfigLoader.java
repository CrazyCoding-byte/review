package com.yzx.crazycodingbytemq.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @className: ConfigLoader
 * @author: yzx
 * @date: 2025/11/14 12:53
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class ConfigLoader {
    private static final String DEFAULT_CONFIG = "mq-default.conf";
    private static Config config;

    static {
        try {
            // 1. 加载默认配置
            Config defaultConfig = ConfigFactory.load(DEFAULT_CONFIG);
            // 2. 加载系统配置（优先级高于文件）
            Config systemConfig = ConfigFactory.systemProperties();
            // 3. 加载外部配置（如果指定）
            Config externalConfig = ConfigFactory.empty();
            String externalPath = System.getProperty("mq.config.path");
            if (externalPath != null) {
                File externalFile = new File(externalPath);
                if (externalFile.exists()) {
                    externalConfig = ConfigFactory.parseFile(externalFile);
                } else {
                    log.warn("外部配置文件不存在：{}，忽略", externalPath);
                }
            }
            // 修复：配置优先级（系统配置 > 外部配置 > 默认配置）
            config = systemConfig.withFallback(externalConfig).withFallback(defaultConfig).resolve();
        } catch (Exception e) {
            throw new RuntimeException("初始化配置失败", e);
        }
    }


    public static Config getRawConfig() {
        return config;
    }

    /**
     * 绑定配置类（修复Duration等复杂类型解析）
     */
    @SneakyThrows
    public static <T> T bindConfig(Class<T> clazz, String path) {
        // 修复：使用ConfigBeanFactory确保Duration等类型正确转换
        return ConfigBeanFactory.create(config.getConfig(path), clazz);
    }

}
