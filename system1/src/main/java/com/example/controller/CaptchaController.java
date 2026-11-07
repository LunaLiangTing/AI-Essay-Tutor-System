package com.example.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    // 简单实现：返回 Base64 图片和验证码文本
    // 实际生产中应该把 code 存入 Redis 并设置过期时间，这里为了演示只返回前端
    @GetMapping("/get")
    public Map<String, String> getCaptcha() {
        // 宽 120，高 40，字符数 4，干扰线 2 条
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 2);

        Map<String, String> result = new HashMap<>();
        // 验证码图片的 Base64 编码
        result.put("img", captcha.getImageBase64Data());
        // 验证码真实的值（实际开发中校验通过后应销毁，这里为了前后端联调暂时返回）
        result.put("code", captcha.getCode());

        return result;
    }
}