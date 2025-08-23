package com.taebin.travelsay.domain.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TripItemRepository extends JpaRepository<TripItem, Long> {

    List<TripItem> findByTripDayIdOrderByOrderNoAsc(Long dayId);

    int countByTripDayId(Long dayId);

    List<TripItem> findByTripDayIdAndOrderNoBetweenOrderByOrderNoAsc(Long dayId, int start, int end);

    void deleteByTripDayId(Long dayId);
    void deleteByTripDayIdIn(Collection<Long> dayIds);

    @Modifying
    @Query("delete from TripItem t where t.tripDay.tripPlan.id = :planId")
    int deleteByTripPlanId(@Param("planId") Long planId);

    @Modifying
    @Query(value = """
    UPDATE trip_item
       SET order_no = order_no + :offset
     WHERE trip_day_id = :dayId
       AND order_no BETWEEN :start AND :end
""", nativeQuery = true)
    int offsetUp(@Param("dayId") Long dayId,
                 @Param("start") int start,
                 @Param("end") int end,
                 @Param("offset") int offset);

    @Modifying
    @Query(value = """
    UPDATE trip_item
       SET order_no = order_no - :down
     WHERE trip_day_id = :dayId
       AND order_no BETWEEN :startPlus AND :endPlus
""", nativeQuery = true)
    int normalizeAfterOffset(@Param("dayId") Long dayId,
                             @Param("startPlus") int startPlus,
                             @Param("endPlus") int endPlus,
                             @Param("down") int down);
}
