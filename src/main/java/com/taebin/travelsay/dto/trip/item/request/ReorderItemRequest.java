package com.taebin.travelsay.dto.trip.item.request;

import jakarta.validation.constraints.NotNull;

public record ReorderItemRequest (
        @NotNull
        Integer newOrderNo
){
}
