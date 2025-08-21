package com.taebin.travelsay.domain.trip;

import com.taebin.travelsay.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(
        name = "trip_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_item_order", columnNames = {"trip_day_id", "order_no"}),
        indexes = @Index(name = "ix_item_day_order", columnList = "trip_day_id, order_no")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

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

    public static TripItem create(TripDay tripDay, String title, LocalTime startTime,
                                  Integer amount, String merchant, String memo, Integer orderNo) {
        if (tripDay == null) throw new IllegalArgumentException("tripDay is null");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is null or empty");
        if (title.length() > 200) throw new IllegalArgumentException("title is too long");
        if (orderNo < 1) throw new IllegalArgumentException("orderNo must be greater than 1");

        TripItem i = new TripItem();
        i.tripDay = tripDay;
        i.title = title;
        i.startTime = startTime;
        i.amount = amount;
        i.merchant = merchant;
        i.memo = memo;
        i.orderNo = orderNo;
        return i;
    }

    public void update(String title, LocalTime startTime, Integer amount, String merchant, String memo) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is null or empty");
        if (title.length() > 200) throw new IllegalArgumentException("title is too long");
        this.title = title;
        this.startTime = startTime;
        this.amount = amount;
        this.merchant = merchant;
        this.memo = memo;
    }

    public void setOrderNo(Integer orderNo) {
        if (orderNo < 1) throw new IllegalArgumentException("orderNo must be greater than 1");
        this.orderNo = orderNo;
    }

    public void changeDay(TripDay newDay) {
        if (newDay == null) throw new IllegalArgumentException("newDay is null");
        this.tripDay = newDay;
    }

}
