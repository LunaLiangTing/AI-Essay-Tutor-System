package com.example.controller;

import com.example.common.Result;
import com.example.service.TencentTtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    @Autowired
    private TencentTtsService tencentTtsService;

    @PostMapping("/synthesize")
    public Result synthesize(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");

            // 获取前端传来的语速
            float speed = 1.0f;
            if (request.containsKey("speed")) {
                speed = Float.parseFloat(request.get("speed").toString());
            }

            // 🔥 注意这里：必须传 text 和 speed 两个参数！
            String audioBase64 = tencentTtsService.synthesize(text, speed);

            return Result.success(audioBase64);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("语音合成失败：" + e.getMessage());
        }
    }
}