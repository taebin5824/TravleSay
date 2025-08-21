package com.taebin.travelsay.dto.trip.plan.response;

public record PlanResponse (
        Long id,
        String title,
        boolean isPublic,
        boolean isCompleted
){
}
