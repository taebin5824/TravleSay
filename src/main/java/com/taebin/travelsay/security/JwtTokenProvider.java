package com.taebin.travelsay.security;

import com.taebin.travelsay.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final JwtParser parser;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        byte[] decoded = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(decoded);

        this.parser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
    }

    public String createToken(String memberId, String loginId, int tokenVersion) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiration = Date.from(now.plusSeconds(jwtProperties.getExpirationMinutes() * 60L));

        return Jwts.builder()
                .setSubject(loginId)
                .claim("mid", memberId)
                .claim("ver", tokenVersion)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validate(String token) {
        try {
            parser.parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getMemberId(String token) {
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims.get("mid", String.class); // 없으면 null
    }

    public String getUsername(String token) {
        return parser.parseClaimsJws(token).getBody().getSubject();
    }

    /** ver(claim) → tokenVersion (Number 전반 안전 처리) */
    public Integer getVersion(String token) {
        Claims claims = parser.parseClaimsJws(token).getBody();
        Object v = claims.get("ver");
        if (v instanceof Number) return ((Number) v).intValue();
        return null;
    }

    public String headerName() {
        return jwtProperties.getHeader();
    }
}
