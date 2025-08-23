package com.taebin.travelsay.dto.trip.plan.response;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PlanDetailResponse(
        Long planId,
        String title,
        Boolean isPublic,
        Boolean isCompleted,
        List<DayRow> days
) {
    public record DayRow(Long dayId, LocalDate tripDate, List<ItemRow> items) {}
    public record ItemRow(Long id, LocalTime startTime, String title, Integer amount, String merchant, String memo) {}
}