package com.nudgeup.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

// TODO: FCM(Firebase Cloud Messaging) 초기화 설정
// - @PostConstruct로 앱 시작 시 FirebaseApp.initializeApp() 1회 실행
// - 서비스 계정 키 파일 경로: src/main/resources/firebase-service-account.json (gitignore 등록 필수)
// - firebase-admin 의존성 build.gradle.kts에 추가 필요:
//   implementation("com.google.firebase:firebase-admin:9.2.0")
@Configuration
public class FcmConfig {

    @Value("${fcm.credentials-path:classpath:firebase-service-account.json}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() throws IOException {
        // TODO: 구현
        // InputStream serviceAccount = ... (credentialsPath 로드)
        // FirebaseOptions options = FirebaseOptions.builder()
        //     .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        //     .build();
        // if (FirebaseApp.getApps().isEmpty()) {
        //     FirebaseApp.initializeApp(options);
        // }
    }
}
