package com.example.controller;

import com.example.common.Result;
import com.example.service.TencentAsrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
//用于接受前端上传的音频文件
@Slf4j // 加上这个注解
@RestController
@RequestMapping("/api/asr")
public class AsrController {

    @Autowired
    private TencentAsrService tencentAsrService;

    @PostMapping("/recognize")
    public Result recognize(@RequestParam("file") MultipartFile file) {
        try {
            byte[] audioData = file.getBytes();
            String text = tencentAsrService.recognize(audioData);
            return Result.success(text);
        } catch (Exception e) { // 🔥 统一 catch Exception
            log.error("语音识别发生异常", e); // 🔥 打印真实堆栈日志
            return Result.error("语音识别失败：" + e.getMessage());
        }
    }
}