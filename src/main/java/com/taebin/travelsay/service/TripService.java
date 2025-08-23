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
import com.taebin.travelsay.dto.trip.plan.response.MyPlanRow;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        tripItemRepository.deleteByTripPlanId(plan.getId());
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
    public TripItem addItem(Long dayId, CreateItemRequest req, String memberId) {
        TripDay day = tripDayRepository.findById(dayId).orElseThrow(() -> new EntityNotFoundException("day"));
        ensurePlanOwnership(day.getTripPlan(), memberId);

        int last = tripItemRepository.countByTripDayId(dayId);
        int orderNo = (req.orderNo() == null) ? last + 1 : req.orderNo();
        if (orderNo < 1 || orderNo > last + 1) throw new IllegalArgumentException("orderNo 범위 오류");

        if (orderNo <= last) {
            shift(dayId, orderNo, last, +1);
        }

        TripItem item = TripItem.create(day, req.title(), req.startTime(),
                req.amount(), req.merchant(), req.memo(), orderNo);

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
            shift(tripDayId, removedOrderNo, lastOrderNo, -1);
        }
    }

    public void reorder(Long itemId, int newOrderNo, String memberId) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("item"));
        ensurePlanOwnership(item.getTripDay().getTripPlan(), memberId);

        Long dayId = item.getTripDay().getId();
        int cur = item.getOrderNo();
        int last = tripItemRepository.countByTripDayId(dayId);

        if (newOrderNo < 1 || newOrderNo > last) throw new IllegalArgumentException("orderNo 범위 오류");
        if (newOrderNo == cur) return;

        parkItemTemporarily(item);

        if (newOrderNo < cur) {
            shift(dayId, newOrderNo, cur - 1, +1);
        } else {
            shift(dayId, cur + 1, newOrderNo, -1);
        }
        item.setOrderNo(newOrderNo);
        tripItemRepository.flush();
    }


    // Day 간 이동
    @Transactional
    public void moveItem(Long itemId, Long targetDayId, Integer newOrderNo, String memberId) {
        TripItem item = tripItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("item"));
        ensurePlanOwnership(item.getTripDay().getTripPlan(), memberId);

        Long srcDayId = item.getTripDay().getId();
        int srcCur = item.getOrderNo();
        int srcLast = tripItemRepository.countByTripDayId(srcDayId);

        if (srcDayId.equals(targetDayId)) {
            if (newOrderNo != null) reorder(itemId, newOrderNo, memberId);
            return;
        }

        TripDay targetDay = tripDayRepository.findById(targetDayId)
                .orElseThrow(() -> new EntityNotFoundException("day"));
        if (!item.getTripDay().getTripPlan().getId().equals(targetDay.getTripPlan().getId())) {
            throw new IllegalArgumentException("다른 플랜으로 이동할 수 없습니다.");
        }

        int targetLast = tripItemRepository.countByTripDayId(targetDayId);
        int pos = (newOrderNo == null) ? targetLast + 1 : newOrderNo;
        if (pos < 1 || pos > targetLast + 1) throw new IllegalArgumentException("orderNo 범위 오류");

        // 타깃 자리 먼저 확보
        if (pos <= targetLast) {
            shift(targetDayId, pos, targetLast, +1);
        }

        // 대상 피신 (소스 day의 유니크 회피)
        parkItemTemporarily(item);

        // Day 이동 + 최종 order 세팅 (타깃 day 유니크도 이미 자리 확보로 안전)
        item.moveTo(targetDay, pos);
        tripItemRepository.flush();

        // 소스 day 빈자리 당기기
        if (srcCur < srcLast) {
            shift(srcDayId, srcCur + 1, srcLast, -1);
        }
    }

    @Transactional(readOnly = true)
    public List<MyPlanRow> getMyPlans(String memberId) {
        return tripPlanRepository.findMyPlansAll(memberId);
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


    private static final int BIG = 100_000;


    private void shift(Long dayId, int start, int end, int delta) {
        if (start > end || delta == 0) return;


        tripItemRepository.offsetUp(dayId, start, end, BIG);
        tripItemRepository.flush();

        int down = BIG - delta;
        tripItemRepository.normalizeAfterOffset(dayId, start + BIG, end + BIG, down);
        tripItemRepository.flush();
    }


    private void parkItemTemporarily(TripItem item) {
        item.setOrderNo(BIG * 2); // NOT NULL만 만족하면 OK
        tripItemRepository.flush();
    }


    @Transactional(readOnly = true)
    public TripPlan getOwnedPlanOrThrow(Long planId, String memberId) {
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
