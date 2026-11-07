package com.example.service;

import com.example.utils.TencentTtsSignUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//流式 TTS 服务 (StreamingTtsService.java),使用 Java 11 自带的 java.net.http.WebSocket 作为客户端，连接腾讯云并透传数据。
@Service
public class StreamingTtsService {
    // 🔥 从 yml 注入
    @Value("${tencent.tts.app-id}")
    private String appId;

    @Value("${tencent.asr.secret-id}")
    private String secretId;

    @Value("${tencent.asr.secret-key}")
    private String secretKey;

    // 🔥 JDK 17 核心：创建一个虚拟线程执行器，每个 TTS 任务独立虚拟线程
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void streamTts(String text, WebSocketSession frontendSession) {
        // 将整个 TTS 流程提交到线程池，不阻塞 Tomcat 平台线程
        executor.submit(() -> {
            try {
                long timestamp = System.currentTimeMillis() / 1000;
                long expired = timestamp + 3600;

                // 🔥 1. 核心修复：确保 appId 是纯数字，去空格，防止 10001 报错
                String cleanAppId = String.valueOf(Long.parseLong(appId.trim()));
                String cleanSecretId = secretId.trim();
                String cleanSecretKey = secretKey.trim();

                Map<String, String> params = Map.of(
                        "Action", "TextToStreamAudioWSv2",
                        "AppId", cleanAppId,
                        "SecretId", cleanSecretId,
                        "Timestamp", String.valueOf(timestamp),
                        "Expired", String.valueOf(expired),
                        "VoiceType", "403001",
                        "Codec", "pcm",
                        "SampleRate", "16000",
                        "Speed", "0"
                );

                String sign = TencentTtsSignUtil.generateSign(params, cleanSecretKey);
                StringBuilder urlBuilder = new StringBuilder("wss://tts.cloud.tencent.com/stream_wsv2?");
                params.forEach((k, v) -> {
                    try {
                        urlBuilder.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8.name())).append('&');
                    } catch (Exception e) { e.printStackTrace(); }
                });
                // 修正后
                urlBuilder.append("Signature=").append(URLEncoder.encode(sign, StandardCharsets.UTF_8));

                // 🔥 打印完整 URL，方便你排错（注意看里面的 AppId 是不是纯数字）
                System.out.println("请求腾讯云的完整 URL: " + urlBuilder.toString());

                // 1. 清理文本：去掉多余换行，确保以句号/问号/感叹号结尾
                String cleanText = text.replace("\n", " ").replace("\r", " ").trim();
                if (!cleanText.matches(".*[。？！?!]$")) {
                    cleanText += "。";
                }
                // 将清理后的文本变为 final，以便在内部类中访问
                final String finalText = cleanText;

                // 2. 使用数组作为标志位，确保在收到 ready 信号后只发送一次文本
                final boolean[] textSent = {false};

                HttpClient client = HttpClient.newHttpClient();
                client.newWebSocketBuilder()
                        .buildAsync(URI.create(urlBuilder.toString()), new WebSocket.Listener() {
                            @Override
                            public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
                                try {
                                    if (frontendSession.isOpen()) {
                                        byte[] bytes = new byte[data.remaining()];
                                        data.get(bytes);
                                        frontendSession.sendMessage(new BinaryMessage(bytes));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                webSocket.request(1);
                                return null;
                            }

                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                System.out.println("TTS status: " + data);

                                try {
                                    // 3. 解析服务端返回的 JSON 状态
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode jsonNode = mapper.readTree(data.toString());

                                    // 4. 判断是否收到 ready == 1 的信号，且还未发送过文本
                                    if (jsonNode.has("ready") && jsonNode.get("ready").asInt() == 1 && !textSent[0]){
                                        textSent[0] = true;

                                        // 5. 构造腾讯云要求的 JSON 指令
                                        Map<String, String> payload = Map.of(
                                                "session_id", UUID.randomUUID().toString(),
                                                "message_id", UUID.randomUUID().toString(),
                                                "action", "ACTION_SYNTHESIS",
                                                "data", finalText
                                        );
                                        String jsonText = mapper.writeValueAsString(payload);

                                        // 6. 发送 JSON 文本（不是裸文本！）
                                        webSocket.sendText(jsonText, true);
                                        System.out.println("🔥 已收到 ready 信号，文本已成功发送！");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                webSocket.request(1);
                                return null;
                            }

                            // 🔥 4. 新增 onError 回调，如果连接被腾讯云拒绝（比如鉴权失败），这里会报出来
                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                System.err.println("====== WebSocket 发生异常 ======");
                                if (error != null) {
                                    error.printStackTrace();
                                    if (error.getCause() != null) {
                                        error.getCause().printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                try {
                                    if (frontendSession.isOpen()) {
                                        frontendSession.sendMessage(new TextMessage("{\"event\":\"end\"}"));
                                    }
                                } catch (Exception ignored) {}
                                return null;
                            }
                        })
                        .thenAccept(webSocket -> {
                            // 🔥 这里不再发送文本，只打印连接成功的日志，等待 ready 回调
                            System.out.println("WebSocket 连接已建立，等待腾讯云 ready 信号...");
                        })
                        .join(); // 等待 WebSocket 连接建立

            } catch (Exception e) {
                System.err.println("TTS 执行出现异常: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}