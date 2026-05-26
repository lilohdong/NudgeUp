package com.nudgeup.backend.global.fcm;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class FcmMessageDto {

    private String token;
    private String title;
    private String body;
    private Map<String, String> data;
}
