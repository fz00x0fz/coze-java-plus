package com.coze.openapi.config;

import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CozeConfiguration {

    @Bean
    public TokenAuth tokenAuth(CozeProperties properties) {
        return new TokenAuth(properties.getApiKey());
    }

    @Bean
    public CozeAPI cozeAPI(TokenAuth tokenAuth, CozeProperties properties) {
        return new CozeAPI.Builder()
                .baseURL(properties.getBaseUrl())
                .auth(tokenAuth)
                .readTimeout(properties.getReadTimeout())
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }
}
