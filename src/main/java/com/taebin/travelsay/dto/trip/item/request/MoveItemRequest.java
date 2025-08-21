package com.taebin.travelsay.dto.trip.item.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveItemRequest (
        @NotNull
        Long targetDayId,
        @Min(1) Integer newOrderNo
){
}
