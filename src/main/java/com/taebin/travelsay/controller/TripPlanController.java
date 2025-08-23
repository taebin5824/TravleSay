package com.taebin.travelsay.controller;


import com.taebin.travelsay.domain.trip.TripDay;
import com.taebin.travelsay.domain.trip.TripPlan;
import com.taebin.travelsay.dto.trip.day.request.CreateDayRequest;
import com.taebin.travelsay.dto.trip.day.response.DayResponse;
import com.taebin.travelsay.dto.trip.plan.request.CreatePlanRequest;
import com.taebin.travelsay.dto.trip.plan.request.UpdatePlanRequest;
import com.taebin.travelsay.dto.trip.plan.response.MyPlanRow;
import com.taebin.travelsay.dto.trip.plan.response.PlanDetailResponse;
import com.taebin.travelsay.dto.trip.plan.response.PlanResponse;
import com.taebin.travelsay.security.MemberDetails;
import com.taebin.travelsay.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/trips/plans")
@RequiredArgsConstructor
public class TripPlanController {

    private final TripService tripService;

    private String mid(MemberDetails memberDetails) {
        if (memberDetails == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return memberDetails.getMemberId();
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyPlanRow>> myPlan(@AuthenticationPrincipal MemberDetails principal) {

        List<MyPlanRow> rows = tripService.getMyPlans(mid(principal));

        return ResponseEntity.ok(rows);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable Long planId,
                                                @AuthenticationPrincipal MemberDetails principal) {
        TripPlan p = tripService.getOwnedPlanOrThrow(planId, mid(principal));
        return ResponseEntity.ok(new PlanResponse(p.getId(), p.getTitle(), p.isPublic(), p.isCompleted()));
    }

    @GetMapping("/{planId}/detail")
    public ResponseEntity<PlanDetailResponse> getPlanDetail(
            @PathVariable Long planId,
            @AuthenticationPrincipal MemberDetails principal
    ) {
        String memberId = mid(principal);

        TripPlan plan = tripService.getOwnedPlanOrThrow(planId, memberId);


        List<TripDay> days = tripService.listDays(planId, memberId);

        // Day + Item 매핑
        List<PlanDetailResponse.DayRow> dayRows = days.stream().map(d -> {
            java.util.List<com.taebin.travelsay.domain.trip.TripItem> items =
                    tripService.listItems(d.getId(), memberId);

            List<PlanDetailResponse.ItemRow> itemRows = items.stream()
                    .map(i -> new PlanDetailResponse.ItemRow(
                            i.getId(),
                            i.getStartTime(),
                            i.getTitle(),
                            i.getAmount(),
                            i.getMerchant(),
                            i.getMemo()
                    ))
                    .toList();

            return new PlanDetailResponse.DayRow(d.getId(), d.getTripDate(), itemRows);
        }).toList();

        PlanDetailResponse body = new PlanDetailResponse(
                plan.getId(),
                plan.getTitle(),
                plan.isPublic(),
                plan.isCompleted(),
                dayRows
        );

        return ResponseEntity.ok(body);
    }



    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request,
                                                   @AuthenticationPrincipal MemberDetails principal) {
        TripPlan plan = tripService.createPlan(mid(principal), request);
        return ResponseEntity
                .created(URI.create("/api/trips/plans/" + plan.getId()))
                .body(new PlanResponse(plan.getId(), plan.getTitle(), plan.isPublic(), plan.isCompleted()));
    }

    @PatchMapping("/{planId}")
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable Long planId,
                                                   @Valid @RequestBody UpdatePlanRequest request,
                                                   @AuthenticationPrincipal MemberDetails principal) {
        TripPlan plan = tripService.updatePlan(planId, request, mid(principal));
        return ResponseEntity.ok(new PlanResponse(plan.getId(), plan.getTitle(), plan.isPublic(), plan.isCompleted()));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long planId,
                                           @AuthenticationPrincipal MemberDetails principal) {
        tripService.deletePlan(planId, mid(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{planId}/days")
    public ResponseEntity<List<DayResponse>> listDays(@PathVariable Long planId,
                                                      @AuthenticationPrincipal MemberDetails principal) {
        List<TripDay> days = tripService.listDays(planId, mid(principal));
        return ResponseEntity.ok(days.stream()
                .map(d -> new DayResponse(d.getId(), d.getTripDate()))
                .toList());
    }

    @PostMapping("/{planId}/days")
    public ResponseEntity<DayResponse> addDay(@PathVariable Long planId,
                                              @Valid @RequestBody CreateDayRequest request,
                                              @AuthenticationPrincipal MemberDetails principal){
        TripDay day = tripService.addDay(planId, request.tripDate(), mid(principal));
        return ResponseEntity
                .created(URI.create("/api/trips/days/" + day.getId()))
                .body(new DayResponse(day.getId(), day.getTripDate()));
    }
}
