package com.nudgeup.app.global.network;

import com.nudgeup.app.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 에뮬레이터: 10.0.2.2 (호스트 PC localhost)
    // 실기기:    PC의 LAN IP로 변경 (예: 192.168.0.10)
    //            → ipconfig(Windows) 또는 ifconfig(Mac/Linux) 로 확인
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit instance;

    public static synchronized Retrofit getInstance() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // 릴리스 빌드에서 BODY 로그 비활성화 (JWT 토큰 등 민감 정보 노출 방지)
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor())
                    .addInterceptor(logging)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }
}
