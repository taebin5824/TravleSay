package com.taebin.travelsay.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record InactivateMemberRequest(
        @NotBlank String password,
        @NotBlank String passwordConfirm,
        @AssertTrue boolean agreed
) {
}
