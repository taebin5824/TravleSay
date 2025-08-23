package com.taebin.travelsay.domain.trip;

import com.taebin.travelsay.dto.trip.plan.response.MyPlanRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

    @Query("""
        select new com.taebin.travelsay.dto.trip.plan.response.MyPlanRow(
            p.id,
            min(d.tripDate),
            p.title,
            p.isPublic,
            p.isCompleted
        )
        from TripPlan p
        left join TripDay d on d.tripPlan = p
        where p.member.memberId = :memberId
        group by p.id, p.title, p.isPublic
        order by case when min(d.tripDate) is null then 1 else 0 end, min(d.tripDate) desc
    """)
    List<MyPlanRow> findMyPlansAll(@Param("memberId") String memberId);
}
