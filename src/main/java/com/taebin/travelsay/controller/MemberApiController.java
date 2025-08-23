package com.taebin.travelsay.controller;

import com.taebin.travelsay.dto.member.request.InactivateMemberRequest;
import com.taebin.travelsay.dto.member.request.LoginRequest;
import com.taebin.travelsay.dto.member.request.SignupRequest;
import com.taebin.travelsay.dto.member.request.UpdateProfileRequest;
import com.taebin.travelsay.dto.member.response.AuthResponse;
import com.taebin.travelsay.dto.member.response.MeResponse;
import com.taebin.travelsay.dto.member.response.UpdateProfileResponse;
import com.taebin.travelsay.security.MemberDetails;
import com.taebin.travelsay.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Integer v) {
            tv = v;
        }
        if (tv == null) {
            Object v = request.getAttribute("jwtVersion");
            if (v instanceof Integer) tv = (Integer) v;
        }

        memberService.logoutByMemberId(principal.getMemberId(), tv);
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
        if (memberDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UpdateProfileResponse res = memberService.updateProfile(memberDetails.getUsername(), req);
        return ResponseEntity.ok(res);
    }
}
