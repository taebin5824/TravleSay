package com.taebin.travelsay.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = "spring.jwt")
public class JwtProperties {

    @NotBlank
    private final String secret;

    @Min(1)
    private final long expirationMinutes;

    @NotBlank
    private final String issuer;

    @NotBlank
    private final String header;

    @NotBlank
    private final String prefix;

    public JwtProperties(String secret, long expirationMinutes, String issuer, String header, String prefix) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
        this.header = header;
        this.prefix = prefix;
    }
}
