package com.example.service;

import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class TencentAsrService {

    @Value("${tencent.asr.secret-id}")
    private String secretId;

    @Value("${tencent.asr.secret-key}")
    private String secretKey;

    /**
     * 核心识别方法：接收前端传来的音频字节，转码后发给腾讯云
     */
    public String recognize(byte[] originalAudioData) throws Exception {
        // 1. 使用 FFmpeg 将音频转码为 16k 单声道 WAV
        byte[] wavData = convertTo16kWav(originalAudioData);

        // 2. 实例化认证对象
        Credential cred = new Credential(secretId, secretKey);

        // 3. 实例化 HTTP 选项
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("asr.tencentcloudapi.com");

        // 4. 实例化客户端配置
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        // 5. 实例化 AsrClient（注意：这里解决了你 "Cannot resolve symbol 'client'" 的问题）
        AsrClient client = new AsrClient(cred, "ap-shanghai", clientProfile);

        // 6. 构造请求参数
        SentenceRecognitionRequest req = new SentenceRecognitionRequest();
        req.setEngSerViceType("16k_zh"); // 引擎类型
        req.setSourceType(1L);           // 数据流
        req.setVoiceFormat("wav");       // 转码后是 wav
        req.setData(Base64.getEncoder().encodeToString(wavData)); // Base64 编码

        // 7. 发送请求并返回结果
        SentenceRecognitionResponse resp = client.SentenceRecognition(req);
        return resp.getResult();
    }

    /**
     * 音频转码方法：将浏览器的 webm 等格式转为腾讯云要求的 16k WAV
     */
    private byte[] convertTo16kWav(byte[] originalAudioData) throws IOException, InterruptedException {
        // 创建临时文件
        Path tempInput = Files.createTempFile("asr_input_", ".webm");
        Path tempOutput = Files.createTempFile("asr_output_", ".wav");

        try {
            Files.write(tempInput, originalAudioData);

            // 调用 FFmpeg 命令行
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", tempInput.toString(),
                    "-ar", "16000",       // 采样率 16k
                    "-ac", "1",           // 单声道
                    "-sample_fmt", "s16", // 16bit 位深
                    tempOutput.toString()
            );
            pb.redirectErrorStream(true);

            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new IOException("无法启动 FFmpeg，请确认是否已安装并配置到系统环境变量 Path 中，且【重启了 IDEA】。", e);
            }

            // 🔥 核心修复：必须读取 FFmpeg 的输出流，否则会卡死，同时打印日志方便排查
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg 运行日志: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 音频转码失败，退出码：" + exitCode);
            }

            // 读取转码后的 WAV 字节
            return Files.readAllBytes(tempOutput);
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
        }
    }
}