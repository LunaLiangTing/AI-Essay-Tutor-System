package com.example.handler;

import com.example.service.StreamingTtsService;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
//前端通信端点 (TtsWebSocketHandler.java):暴露一个 WebSocket 端点给前端，接收文本并调用服务。
public class TtsWebSocketHandler extends TextWebSocketHandler {
    private final StreamingTtsService ttsService;

    public TtsWebSocketHandler(StreamingTtsService ttsService) {
        this.ttsService = ttsService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String text = message.getPayload();
//        加上一行打印，看看前端到底发了什么过来
        System.out.println("【后端 TTS 收到文本】: [" + text + "]");
        ttsService.streamTts(text, session);
    }
}