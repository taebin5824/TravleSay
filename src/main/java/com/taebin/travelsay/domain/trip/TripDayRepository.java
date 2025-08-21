package com.taebin.travelsay.domain.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    boolean existsByTripPlanIdAndTripDate(Long tripPlanId, LocalDate tripDate);
    List<TripDay> findByTripPlanIdOrderByTripDateAsc(Long tripPlanId);

    @Query("select d.id from TripDay d where d.tripPlan.id = :planId")
    List<Long> findIdsByPlan(@Param("planId") Long planId);

    void deleteByTripPlanId(Long planId);
}
