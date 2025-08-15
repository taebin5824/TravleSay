package com.taebin.travelsay.dto;

public record MeResponse (
        String memberId,
        String loginId,
        String email,
        String role,
        String status
){}
