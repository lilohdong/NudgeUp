package com.nudgeup.backend.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// TODO: application.yaml의 jwt 설정값을 바인딩하는 프로퍼티 클래스
// - yaml에 jwt.secret, jwt.access-token-expiry, jwt.refresh-token-expiry 추가 필요
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    // TODO: application.yaml에 아래 값 추가
    // jwt:
    //   secret: ${JWT_SECRET}
    //   access-token-expiry: 3600000     # 1시간
    //   refresh-token-expiry: 1209600000 # 14일
    private String secret;
    private long accessTokenExpiry;
    private long refreshTokenExpiry;
}
