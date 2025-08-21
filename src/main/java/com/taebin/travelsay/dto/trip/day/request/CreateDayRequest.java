package com.taebin.travelsay.dto.trip.day.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDayRequest (
        @NotNull
        LocalDate tripDate
){
}
