package com.taebin.travelsay.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(

        // 아이디: 길이만 제한 (모든 문자 허용)
        @NotBlank
        @Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다.")
        String loginId,

        // 비밀번호: 8~100자
        @NotBlank
        @Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다.")
        String password,

        // 비밀번호 확인: 8~100자
        @NotBlank
        @Size(min = 8, max = 100, message = "비밀번호확인은 8~100자여야 합니다.")
        String passwordConfirm,

        // 이메일 형식
        @NotBlank
        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        String email,

        // 휴대폰번호: 숫자와 하이픈(-) 허용, 9~20자
        @NotBlank
        @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호는 숫자와 '-'만 허용하며 9~20자여야 합니다.")
        String phoneNumber
) {}
