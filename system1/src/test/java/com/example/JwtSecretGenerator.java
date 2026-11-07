package com.example;
import java.security.SecureRandom;
import java.util.Base64;
/**
 * 生成 JWT 安全密钥的工具类
 */
public class JwtSecretGenerator {
    public static void main(String[] args) {
        // 生成 64 字节（512 位）的随机密钥（越长越安全）
        SecureRandom secureRandom = new SecureRandom(); // 加密级别的随机数生成器
        byte[] secretBytes = new byte[64];
        secureRandom.nextBytes(secretBytes);

        // 转成 Base64 字符串（方便配置和使用）
        String jwtSecret = Base64.getEncoder().encodeToString(secretBytes);

        System.out.println("生成的 JWT 安全密钥（复制到配置文件即可）：");
        System.out.println(jwtSecret);
    }
}