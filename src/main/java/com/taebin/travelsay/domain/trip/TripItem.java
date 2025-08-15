package com.taebin.travelsay.domain.trip;

import com.taebin.travelsay.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(
        name = "trip_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_item_order", columnNames = {"trip_dau_id", "order_no"}),
        indexes = @Index(name = "ix_item_day_order", columnList = "trip_day_id, order_no")
)
@Getter
@NoArgsConstructor

public class TripItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "merchant", length = 200)
    private String merchant;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

}
