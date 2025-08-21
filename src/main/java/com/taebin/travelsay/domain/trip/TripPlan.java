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
@NoArgsConstructor(access = AccessLevel.PROTECTED)

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

    public static TripPlan create(Member member, String title, Boolean isPublic) {
        return TripPlan.builder()
                .member(member)
                .title(title)
                .isPublic(false)
                .build();
    }

    public void changeTitle(String title) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("제목은 비어있을 수 없습니다.");

        if (title.length() > 200)
            throw new IllegalArgumentException("제목은 200자 이하여야 합니다.");

        this.title = title;
    }

    public void setVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

}
