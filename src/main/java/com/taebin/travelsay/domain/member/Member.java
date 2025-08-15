package com.taebin.travelsay.domain.member;


import com.taebin.travelsay.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_phone_number", columnNames = "phone_number"),
                @UniqueConstraint(name = "uk_provider_member", columnNames = {"provider", "provider_id"})
        }
)
@Getter
@NoArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @Column(name = "member_id", length = 36, nullable = false, updatable = false)
    private String memberId;

    @PrePersist
    private void prePersist() {
        if (this.memberId == null) {
            this.memberId = UUID.randomUUID().toString();
        }
    }

    @Column(name = "login_id", length = 50, nullable = false)
    private String loginId;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role = MemberRole.USER;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    public void setForSignup(String loginId, String password, String email, String phoneNumber,
                             MemberStatus status, MemberRole role) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.role = role;
    }
}
