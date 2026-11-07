package com.example.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
//签名生成工具类 (TencentTtsSignUtil.java),流式接口的鉴权采用 签名 机制，需要将参数排序拼接后，用 SecretKey 做 HMAC-SHA1 并 Base64 编码。
public class TencentTtsSignUtil {
    public static String generateSign(Map<String, String> params, String secretKey) throws Exception {
        // 1. 参数按字典序排序
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // 去掉末尾的 &
        }

        // 🔥 核心修复：WebSocket 签名必须在前面拼接 GET + host + path + ?
        String strToSign = "GETtts.cloud.tencent.com/stream_wsv2?" + sb.toString();

        // 2. HMAC-SHA1 加密
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signBytes = mac.doFinal(strToSign.getBytes(StandardCharsets.UTF_8));

        // 3. Base64 编码
        return Base64.getEncoder().encodeToString(signBytes);
    }
}