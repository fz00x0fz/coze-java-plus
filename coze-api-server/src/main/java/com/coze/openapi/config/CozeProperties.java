package com.coze.openapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "coze")
public class CozeProperties {
    private String apiKey;
    private String baseUrl = "https://api.coze.cn";
    private int connectTimeout = 10000;
    private int readTimeout = 30000;
}
