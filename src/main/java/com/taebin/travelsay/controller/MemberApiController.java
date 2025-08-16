package com.taebin.travelsay.controller;

import com.taebin.travelsay.dto.*;
import com.taebin.travelsay.security.MemberDetails;
import com.taebin.travelsay.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal MemberDetails principal,
            HttpServletRequest request
    ) {
        if (principal == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Integer tv = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Integer v) {
            tv = v;
        }
        if (tv == null) {
            Object v = request.getAttribute("jwtVersion");
            if (v instanceof Integer) tv = (Integer) v;
        }

        memberService.logoutByMemberId(principal.getMemberId(), tv); // ✅ memberId 사용
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal MemberDetails principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(memberService.getMe(principal.getUsername()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal MemberDetails principal,
            @Valid @RequestBody InactivateMemberRequest req
    ) {
        if (principal == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        memberService.inactivateMember(principal.getMemberId(), req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestBody UpdateProfileRequest req
    ){

        log.warn("[update] DTO class={}, idHash={}, curPw?={}, loginId='{}', pw?={}, pwC?={}, email='{}', phone='{}'",
                req.getClass().getName(),
                System.identityHashCode(req),
                req.currentPassword()!=null && !req.currentPassword().isBlank(),
                req.loginId(),
                req.password()!=null && !req.password().isBlank(),
                req.passwordConfirm()!=null && !req.passwordConfirm().isBlank(),
                req.email(),
                req.phoneNumber());
        if (memberDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UpdateProfileResponse res = memberService.updateProfile(memberDetails.getUsername(), req);
        return ResponseEntity.ok(res);
    }
}
