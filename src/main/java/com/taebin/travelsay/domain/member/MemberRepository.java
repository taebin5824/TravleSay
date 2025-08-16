package com.taebin.travelsay.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByLoginId(String loginId);
    Optional<Member> findByMemberId(String memberId);

    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailAndMemberIdNot(String email, String memberId);
    boolean existsByPhoneNumberAndMemberIdNot(String phoneNumber, String memberId);

    @Query("select m.tokenVersion from Member m where m.loginId = :loginId")
    Integer findTokenVersionByLoginId(@Param("loginId") String loginId);

    @Modifying
    @Query("""
    update Member m
       set m.tokenVersion = m.tokenVersion + 1
     where m.memberId = :memberId
       and m.tokenVersion = :currentVersion
""")
    int bumpIfMatchesByMemberId(@Param("memberId") String memberId,
                                @Param("currentVersion") Integer currentVersion);
}
