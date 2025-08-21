package com.taebin.travelsay.domain.trip;

import com.taebin.travelsay.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Table(
        name = "trip_day",
        uniqueConstraints = @UniqueConstraint(name = "uk_day_date", columnNames = {"trip_plan_id", "trip_date"}),
        indexes = @Index(name = "ix_day_plan_date", columnList = "trip_plan_id, trip_date")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDay extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_day_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_plan_id", nullable = false)
    private TripPlan tripPlan;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    public static TripDay create(TripPlan plan, LocalDate date) {
        if (plan == null) throw new IllegalArgumentException("tripPlan is null");
        if (date == null) throw new IllegalArgumentException("tripDate is null");
        TripDay d = new TripDay();
        d.tripPlan = plan;
        d.tripDate = date;
        return d;
    }
}
