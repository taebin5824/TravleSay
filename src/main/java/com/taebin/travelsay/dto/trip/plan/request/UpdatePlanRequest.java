package com.taebin.travelsay.dto.trip.plan.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePlanRequest (
        @NotBlank
        @Size(max = 200)
        String title,

        @NotNull
        Boolean isPublic,

        @NotNull
        Boolean isCompleted
){
}
