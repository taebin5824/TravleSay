package com.taebin.travelsay.controller;


import com.taebin.travelsay.dto.AuthResponse;
import com.taebin.travelsay.dto.LoginRequest;
import com.taebin.travelsay.dto.MeResponse;
import com.taebin.travelsay.dto.SignupRequest;
import com.taebin.travelsay.security.MemberDetails;
import com.taebin.travelsay.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.status(HttpStatus.OK).body(memberService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal MemberDetails memberDetails) {
        if (memberDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(memberService.getMe(memberDetails.getUsername()));
    }
}
