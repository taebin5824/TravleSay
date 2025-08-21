package com.taebin.travelsay.controller;


import com.taebin.travelsay.domain.trip.TripItem;
import com.taebin.travelsay.dto.trip.item.request.ShiftItemsTimeRequest;
import com.taebin.travelsay.dto.trip.item.response.ItemResponse;
import com.taebin.travelsay.security.MemberDetails;
import com.taebin.travelsay.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/trips/days")
@RequiredArgsConstructor
public class TripDayController {

    private final TripService tripService;

    private String mid(MemberDetails memberDetails) {
        if (memberDetails == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return memberDetails.getMemberId();
    }

    @DeleteMapping("/{dayId}")
    public ResponseEntity<Void> removeDay(@PathVariable Long dayId,
                                          @AuthenticationPrincipal MemberDetails principal) {
        tripService.removeDay(dayId, mid(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{dayId}/items/shift-time")
    public ResponseEntity<List<ItemResponse>> shiftTimes(@PathVariable Long dayId,
                                                         @Valid @RequestBody ShiftItemsTimeRequest request,
                                                         @AuthenticationPrincipal MemberDetails principal) {
        int minutes = parseOffsetToMinutes(request.offset());
        List<TripItem> items = tripService.shiftTimesForDay(dayId, minutes, mid(principal));

        List<ItemResponse> body = items.stream()
                .map(i -> new ItemResponse(
                        i.getId(),
                        i.getTitle(),
                        i.getStartTime(),
                        i.getAmount(),
                        i.getMerchant(),
                        i.getMemo(),
                        i.getOrderNo()
                ))
                .toList();

        return ResponseEntity.ok(body);
    }

    // "HH:mm"  minutes 로 변환.
    private int parseOffsetToMinutes(String offset) {
        boolean negative = offset.startsWith("-");
        String s = offset.replaceFirst("^[+-]", "");
        if ("24:00".equals(s)) return negative ? -1440 : 1440;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
        LocalTime t = LocalTime.parse(s, fmt);
        int minutes = t.getHour() * 60 + t.getMinute();
        return negative ? -minutes : minutes;
    }
}
