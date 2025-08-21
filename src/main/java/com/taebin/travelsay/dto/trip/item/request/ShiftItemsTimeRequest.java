package com.taebin.travelsay.dto.trip.item.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShiftItemsTimeRequest(
        @NotBlank
        @Pattern(
                regexp = "^[+-]?(?:([01]?\\d|2[0-3]):[0-5]\\d|24:00)$",
                message = "offset은 HH:mm 형식이어야 합니다. 예: 01:00, -00:30,"
        )
        String offset
) {
}
