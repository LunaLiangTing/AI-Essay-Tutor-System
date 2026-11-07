package com.example.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
// 因为application.yml 里已经配置了固定的 secret 和过期时间，必须让 JwtUtil 读取这个配置。这样即使重启后端，之前前端保存的 Token 也不会失效。
@Component
public class JwtUtil {

    // 从 application.yml 中读取 secret
    @Value("${system1.jwt.secret}")
    private String secret;

    // 从 application.yml 中读取过期时间（毫秒）
    @Value("${system1.jwt.expiration}")
    private long expiration;

    /**
     * 获取加密密钥
     * 因为你的 secret 是 Base64 编码的，所以这里用 Decoders.BASE64 解码
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     * @param userId 用户ID
     * @param role 角色（TEACHER, STUDENT 等）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 把用户ID存入 subject
                .claim("role", role)                // 自定义 claim 存入角色
                .setIssuedAt(now)                   // 签发时间
                .setExpiration(expiryDate)          // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 解析 Token，获取 Claims
     * @param token JWT 字符串
     * @return Claims 对象（包含了我们存进去的 userId 和 role）
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}