package com.nudgeup.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// TODO: CORS 설정
// - 안드로이드 앱은 CORS 미적용이지만 개발 환경(Postman, Swagger) 대응 목적
// - 허용 origins: * (개발용), 운영 시 앱 서버 도메인으로 제한
// - 허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS
// - allowedHeaders: *
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TODO: 구현
    }
}
