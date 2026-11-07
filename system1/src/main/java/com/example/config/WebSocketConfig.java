package com.example.config;

import com.example.handler.TtsWebSocketHandler;
import com.example.service.StreamingTtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

//当你在 Study.vue 里点击发送，AI 回复了一段文字后，前端会调用 speakText 方法，去连接 ws://localhost:8080/ws/tts。
//
//前端把文字发给后端。
//
//后端通过 StreamingTtsService 发给腾讯云。
//
//腾讯云把语音数据流（PCM）一段一段吐给后端。
//
//后端再把这些音频数据实时推给前端。
//
//前端用 AudioContext 播放出来。
//
//这个文件就是用来开启这个“打电话”的通道的。 如果没有它，你的前端在发送文字后，控制台就会报错 WebSocket connection to 'ws://localhost:8080/ws/tts' failed，AI 就不会有声音了。
@Configuration
@EnableWebSocket // 🔥 这个注解绝对不能少！
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private StreamingTtsService streamingTtsService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册路径 /ws/tts
        registry.addHandler(new TtsWebSocketHandler(streamingTtsService), "/ws/tts")
                .setAllowedOrigins("*"); // 🔥 允许前端跨域连接
    }
}