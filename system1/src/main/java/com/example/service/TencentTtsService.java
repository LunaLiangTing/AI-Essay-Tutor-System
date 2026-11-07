package com.example.service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TencentTtsService {

    @Value("${tencent.asr.secret-id}")
    private String secretId;

    @Value("${tencent.asr.secret-key}")
    private String secretKey;

    public String synthesize(String text, float speed) throws Exception {
        // 1. 实例化认证对象 (注意导包是否正确)
        Credential cred = new Credential(secretId, secretKey);

        // 2. HTTP 选项
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("tts.tencentcloudapi.com");

        // 3. 客户端配置
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        // 4. 实例化 TTS 客户端
        TtsClient client = new TtsClient(cred, "ap-shanghai", clientProfile);

        // 5. 构造请求参数
        TextToVoiceRequest req = new TextToVoiceRequest();
        req.setText(text);
        req.setSessionId(UUID.randomUUID().toString());

        // 🔥 音色 ID：
        // 其他音色 ID 可以在腾讯云控制台查阅（如 101001 是基础女声）
        req.setVoiceType(402000L);
        req.setSpeed(speed);
//        req.setSpeed(0.9f); // 语速，小学生建议 0.8 - 0.9
        req.setVolume(5f); // 音量 0-10
        req.setProjectId(0L);
        req.setModelType(1L); // 1 表示默认模型
        req.setPrimaryLanguage(1L); // 1: 中文

        // 6. 发送请求
        TextToVoiceResponse resp = client.TextToVoice(req);

        // 7. 返回 Base64 编码的音频（前端直接转成音频播放即可）
        return resp.getAudio();
    }
}