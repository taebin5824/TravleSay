package com.taebin.travelsay.dto.trip.day.response;

import java.time.LocalDate;

public record DayResponse (
        Long id,
        LocalDate tripDate
){
}
