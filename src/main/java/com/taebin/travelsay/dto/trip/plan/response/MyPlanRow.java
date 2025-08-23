package com.taebin.travelsay.dto.trip.plan.response;

import java.time.LocalDate;

public record MyPlanRow(
        Long planId,
        LocalDate startDate,
        String title,
        Boolean isPublic,
        Boolean isCompleted
){
}
