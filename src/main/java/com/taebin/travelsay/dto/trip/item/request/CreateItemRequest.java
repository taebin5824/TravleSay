package com.taebin.travelsay.dto.trip.item.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record CreateItemRequest (
        @NotBlank
        @Size(max = 200)
        String title,

        LocalTime startTime,

        Integer amount,

        @Size(max = 200)
        String merchant,

        @Size(max = 500)
        String memo,

        Integer orderNo
){
}
