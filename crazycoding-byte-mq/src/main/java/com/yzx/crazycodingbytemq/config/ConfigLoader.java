package com.yzx.crazycodingbytemq.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
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
        //加载默认配置
        config = ConfigFactory.load(DEFAULT_CONFIG);
        log.info("添加默认配置{}", DEFAULT_CONFIG);
        String externalConfigPath = System.getProperty("mq.config.path");
        if (externalConfigPath != null) {
            //加载外部配置
            File file = new File(externalConfigPath);
            if (file.exists() && file.canRead()) {
                config = ConfigFactory.parseFile(file).withFallback(config);
            }
        }
        config = config.resolve();
    }

    public static Config getRawConfig() {
        return config;
    }

    public static <T> T bindConfig(Class<T> clazz, String path) {
        try {
            Config subConfig = config.getConfig(path);
            return ConfigBeanFactory.create(subConfig, clazz);
        } catch (Exception e) {
            log.error("bindConfig error", e);
            throw new RuntimeException("配置绑定异常", e);
        }

    }

}
