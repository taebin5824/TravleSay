package com.taebin.travelsay.domain.trip;


import com.taebin.travelsay.domain.common.BaseTimeEntity;
import com.taebin.travelsay.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "trip_plan",
        indexes = {
                @Index(name = "ix_plan_member_completed_updated", columnList = "member_id, is_completed, updated_at"),
                @Index(name = "ix_plan_public_updated", columnList = "is_public, updated_at")
        }
)
@Getter
@NoArgsConstructor

public class TripPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_plan_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, referencedColumnName = "member_id")
    private Member member;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Builder
    private TripPlan(Member member, String title, Boolean isPublic, Boolean isCompleted) {
        this.member = member;
        this.title = title;
        this.isPublic = isPublic;
        this.isCompleted = isCompleted;
    }
}
