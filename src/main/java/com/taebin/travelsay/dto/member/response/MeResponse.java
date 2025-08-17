package com.taebin.travelsay.dto.member.response;

public record MeResponse (
        String memberId,
        String loginId,
        String email,
        String phoneNumber,
        String role,
        String status
){}
