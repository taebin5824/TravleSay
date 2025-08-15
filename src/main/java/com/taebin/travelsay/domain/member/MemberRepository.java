package com.taebin.travelsay.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
