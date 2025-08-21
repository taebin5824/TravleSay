package com.taebin.travelsay.domain.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TripItemRepository extends JpaRepository<TripItem, Long> {

    List<TripItem> findByTripDayIdOrderByOrderNoAsc(Long dayId);

    int countByTripDayId(Long dayId);

    List<TripItem> findByTripDayIdAndOrderNoBetweenOrderByOrderNoAsc(Long dayId, int start, int end);

    void deleteByTripDayId(Long DayId);
    void deleteByTripDayIdIn(Collection<Long> dayIds);
}
