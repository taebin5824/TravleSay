package com.taebin.travelsay.dto.member.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateProfileRequest (
        String currentPassword,
        String loginId,
        String password,
        String passwordConfirm,
        String email,
        String phoneNumber
){
}
