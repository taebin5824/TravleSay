package com.taebin.travelsay.service;

import com.taebin.travelsay.domain.member.Member;
import com.taebin.travelsay.domain.member.MemberRepository;
import com.taebin.travelsay.domain.trip.TripDay;
import com.taebin.travelsay.domain.trip.TripDayRepository;
import com.taebin.travelsay.domain.trip.TripItem;
import com.taebin.travelsay.domain.trip.TripItemRepository;
import com.taebin.travelsay.domain.trip.TripPlan;
import com.taebin.travelsay.domain.trip.TripPlanRepository;

import com.taebin.travelsay.dto.trip.item.request.CreateItemRequest;
import com.taebin.travelsay.dto.trip.item.request.UpdateItemRequest;
import com.taebin.travelsay.dto.trip.plan.request.CreatePlanRequest;
import com.taebin.travelsay.dto.trip.plan.request.UpdatePlanRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService {

    private final TripPlanRepository tripPlanRepository;
    private final TripDayRepository tripDayRepository;
    private final TripItemRepository tripItemRepository;
    private final MemberRepository memberRepository;

    // ===== Plan =====
    public TripPlan createPlan(String memberId, CreatePlanRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("member"));
        TripPlan plan = TripPlan.create(member, request.title(), request.isPublic());
        return tripPlanRepository.save(plan);
    }

    public TripPlan updatePlan(Long planId, UpdatePlanRequest request, String memberId) {
        TripPlan plan = getOwnedPlanOrThrow(planId, memberId);
        plan.changeTitle(request.title());
        plan.setVisibility(request.isPublic());
        plan.setCompleted(request.isCompleted());
        return plan;
    }

    public void deletePlan(Long planId, String memberId) {
        TripPlan plan = getOwnedPlanOrThrow(planId, memberId);
        // 단방향이라 명시적 삭제 순서 필요
        tripItemRepository.deleteByTripDayId(plan.getId());
        tripDayRepository.deleteByTripPlanId(plan.getId());
        tripPlanRepository.delete(plan);
    }

    @Transactional(readOnly = true)
    public List<TripDay> listDays(Long planId, String memberId) {
        getOwnedPlanOrThrow(planId, memberId);
        return tripDayRepository.findByTripPlanIdOrderByTripDateAsc(planId);
    }

    // ===== Day =====
    public TripDay addDay(Long planId, LocalDate tripDate, String memberId) {
        TripPlan plan = getOwnedPlanOrThrow(planId, memberId);
        if (tripDayRepository.existsByTripPlanIdAndTripDate(planId, tripDate)) {
            throw new IllegalArgumentException("이미 같은 날짜가 존재합니다.");
        }
        TripDay day = TripDay.create(plan, tripDate);
        return tripDayRepository.save(day);
    }

    public void removeDay(Long dayId, String memberId) {
        TripDay day = tripDayRepository.findById(dayId)
                .orElseThrow(() -> new EntityNotFoundException("day"));
        ensurePlanOwnership(day.getTripPlan(), memberId);

        tripItemRepository.deleteByTripDayId(dayId); // Item 먼저 삭제
        tripDayRepository.delete(day);
    }

    @Transactional(readOnly = true)
    public List<TripItem> listItems(Long tripDayId, String memberId) {
        TripDay day = tripDayRepository.findById(tripDayId)
                .orElseThrow(() -> new EntityNotFoundException("day"));
        ensurePlanOwnership(day.getTripPlan(), memberId);
        return tripItemRepository.findByTripDayIdOrderByOrderNoAsc(tripDayId);
    }

    // ===== Item =====
    // orderNo가 있으면 그 위치에, 없으면 startTime 기준 기본 위치
    public TripItem addItem(Long tripDayId, CreateItemRequest request, String memberId) {
        TripDay day = tripDayRepository.findById(tripDayId)
                .orElseThrow(() -> new EntityNotFoundException("day"));
        ensurePlanOwnership(day.getTripPlan(), memberId);

        int lastOrderNo = tripItemRepository.countByTripDayId(tripDayId);
        int targetPosition;

        if (request.orderNo() != null) {
            if (request.orderNo() < 1 || request.orderNo() > lastOrderNo + 1) {
                throw new IllegalArgumentException("orderNo 범위 오류");
            }
            targetPosition = request.orderNo();
        } else {
            targetPosition = computeInsertPositionByStartTime(tripDayId, request.startTime(), null);
        }

        if (targetPosition <= lastOrderNo) {
            shiftOrderRange(tripDayId, targetPosition, lastOrderNo, +1);
        }

        TripItem item = TripItem.create(day, request.title(), request.startTime(),
                request.amount(), request.merchant(), request.memo(), targetPosition);
        return tripItemRepository.save(item);
    }

    public TripItem updateItem(Long itemId, UpdateItemRequest request, String memberId) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("item"));
        ensurePlanOwnership(item.getTripDay().getTripPlan(), memberId);

        item.update(request.title(), request.startTime(),
                request.amount(), request.merchant(), request.memo());

        return item;
    }

    public void deleteItem(Long itemId, String memberId) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("item"));
        ensurePlanOwnership(item.getTripDay().getTripPlan(), memberId);

        Long tripDayId = item.getTripDay().getId();
        int removedOrderNo = item.getOrderNo();

        tripItemRepository.delete(item);

        int lastOrderNo = tripItemRepository.countByTripDayId(tripDayId);
        if (removedOrderNo <= lastOrderNo) {
            shiftOrderRange(tripDayId, removedOrderNo, lastOrderNo, -1);
        }
    }

    public void reorder(Long itemId, int newOrderNo, String memberId) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("item"));
        ensurePlanOwnership(item.getTripDay().getTripPlan(), memberId);

        Long tripDayId = item.getTripDay().getId();
        int currentOrderNo = item.getOrderNo();
        int lastOrderNo = tripItemRepository.countByTripDayId(tripDayId);

        if (newOrderNo < 1 || newOrderNo > lastOrderNo) {
            throw new IllegalArgumentException("orderNo 범위 오류");
        }
        if (newOrderNo == currentOrderNo) return;

        if (newOrderNo < currentOrderNo) {
            // 위로 이동: [newOrderNo .. currentOrderNo-1] +1
            shiftOrderRange(tripDayId, newOrderNo, currentOrderNo - 1, +1);
        } else {
            // 아래로 이동: [currentOrderNo+1 .. newOrderNo] -1
            shiftOrderRange(tripDayId, currentOrderNo + 1, newOrderNo, -1);
        }
        item.setOrderNo(newOrderNo);
    }

    // Day 간 이동 (같은 Plan 내)
    public void moveItem(Long itemId, Long targetDayId, Integer newOrderNo, String memberId) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("item"));

        TripDay sourceDay = item.getTripDay();
        TripDay targetDay = tripDayRepository.findById(targetDayId)
                .orElseThrow(() -> new EntityNotFoundException("day"));

        ensurePlanOwnership(sourceDay.getTripPlan(), memberId);

        if (!sourceDay.getTripPlan().getId().equals(targetDay.getTripPlan().getId())) {
            throw new IllegalArgumentException("같은 플랜 내에서만 이동할 수 있습니다.");
        }

        Long sourceDayId = sourceDay.getId();
        Long destDayId = targetDay.getId();

        // 같은 Day면 reorder로 처리
        if (sourceDayId.equals(destDayId)) {
            reorder(itemId, (newOrderNo == null ? item.getOrderNo() : newOrderNo), memberId);
            return;
        }

        int sourceOrderNo = item.getOrderNo();
        int sourceLastOrderNo = tripItemRepository.countByTripDayId(sourceDayId);
        int destLastOrderNo = tripItemRepository.countByTripDayId(destDayId);

        int destPosition;
        if (newOrderNo != null) {
            if (newOrderNo < 1 || newOrderNo > destLastOrderNo + 1) {
                throw new IllegalArgumentException("orderNo 범위 오류");
            }
            destPosition = newOrderNo;
        } else {
            destPosition = computeInsertPositionByStartTime(destDayId, item.getStartTime(), null);
        }

        // 1) 목적지에 빈칸 만들기
        if (destPosition <= destLastOrderNo) {
            shiftOrderRange(destDayId, destPosition, destLastOrderNo, +1);
        }

        // 2) Day/순서 변경
        item.changeDay(targetDay);
        item.setOrderNo(destPosition);

        // 3) 출발지 빈칸 당겨오기
        if (sourceOrderNo < sourceLastOrderNo) {
            shiftOrderRange(sourceDayId, sourceOrderNo + 1, sourceLastOrderNo, -1);
        }
    }

    public List<TripItem> shiftTimesForDay(Long tripDayId, int minutes, String memberId) {
        TripDay day = tripDayRepository.findById(tripDayId)
                .orElseThrow(() -> new EntityNotFoundException("day"));
        ensurePlanOwnership(day.getTripPlan(), memberId);

        List<TripItem> items = tripItemRepository.findByTripDayIdOrderByOrderNoAsc(tripDayId);
        if (minutes == 0 || items.isEmpty()) return items;

        final int SECONDS_PER_DAY = 24 * 60 * 60;
        final int deltaSeconds = minutes * 60;

        // 사전 검증: 하나라도 범위 벗어나면 전체 취소
        for (TripItem it : items) {
            if (it.getStartTime() == null) continue;
            int newSec = it.getStartTime().toSecondOfDay() + deltaSeconds;
            if (newSec < 0 || newSec >= SECONDS_PER_DAY) {
                throw new IllegalArgumentException("시간 이동 결과가 하루 범위를 벗어납니다. itemId=" + it.getId());
            }
        }

        for (TripItem it : items) {
            if (it.getStartTime() == null) continue;
            int newSec = it.getStartTime().toSecondOfDay() + deltaSeconds;
            it.update(
                    it.getTitle(),
                    java.time.LocalTime.ofSecondOfDay(newSec),
                    it.getAmount(),
                    it.getMerchant(),
                    it.getMemo()
            );
        }
        return items;
    }

    // ===== Helpers =====
    /** startTime 기준 기본 삽입 위치 계산 (동일시각은 기존 순서 유지, null은 맨 뒤) */
    private int computeInsertPositionByStartTime(Long tripDayId, LocalTime targetTime, Long excludeItemId) {
        List<TripItem> items = tripItemRepository.findByTripDayIdOrderByOrderNoAsc(tripDayId);
        int position = 1;
        for (TripItem it : items) {
            if (excludeItemId != null && it.getId().equals(excludeItemId)) continue;
            LocalTime other = it.getStartTime();
            if (targetTime != null && (other == null || targetTime.isBefore(other))) break;
            position++;
        }
        return position; // 못 찾으면 맨 뒤(last+1)
    }



    private void shiftOrderRange(Long tripDayId, int startOrderNo, int endOrderNo, int delta) {
        if (startOrderNo > endOrderNo) return;
        List<TripItem> affected = tripItemRepository
                .findByTripDayIdAndOrderNoBetweenOrderByOrderNoAsc(tripDayId, startOrderNo, endOrderNo);
        for (TripItem it : affected) {
            it.setOrderNo(it.getOrderNo() + delta);
        }
    }

    private TripPlan getOwnedPlanOrThrow(Long planId, String memberId) {
        TripPlan plan = tripPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("plan"));
        ensurePlanOwnership(plan, memberId);
        return plan;
    }

    private void ensurePlanOwnership(TripPlan plan, String memberId) {
        if (!plan.getMember().getMemberId().equals(memberId)) {
            throw new SecurityException("본인 소유가 아닙니다.");
        }
    }
}
