package com.taebin.travelsay.dto.trip.item.response;

import java.time.LocalTime;

public record ItemResponse (
        Long id,
        String title,
        LocalTime startTime,
        Integer amount,
        String merchant,
        String memo,
        Integer orderNo
){
}
