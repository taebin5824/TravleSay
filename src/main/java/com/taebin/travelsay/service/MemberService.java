package com.taebin.travelsay.service;


import com.taebin.travelsay.domain.member.Member;
import com.taebin.travelsay.domain.member.MemberRepository;
import com.taebin.travelsay.domain.member.MemberRole;
import com.taebin.travelsay.domain.member.MemberStatus;
import com.taebin.travelsay.dto.AuthResponse;
import com.taebin.travelsay.dto.LoginRequest;
import com.taebin.travelsay.dto.MeResponse;
import com.taebin.travelsay.dto.SignupRequest;
import com.taebin.travelsay.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm()))
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");

        if (memberRepository.existsByLoginId(request.loginId()))
            throw new IllegalArgumentException("이미 사용중인 아이디입니다.");

        if (memberRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");

        if (memberRepository.existsByPhoneNumber(request.phoneNumber()))
            throw new IllegalArgumentException("이미 사용중인 전화번호입니다.");

        String encoded = passwordEncoder.encode(request.password());

        Member member = new Member();
        member.setForSignup(
                request.loginId(),
                encoded,
                request.email(),
                request.phoneNumber(),
                MemberStatus.ACTIVE,
                MemberRole.USER
        );
        memberRepository.save(member);
    }

    public AuthResponse login(LoginRequest request) {
        var token = new UsernamePasswordAuthenticationToken(request.loginId(), request.password());
        authenticationManager.authenticate(token);
        String jwt = jwtTokenProvider.createToken(request.loginId());
        return new AuthResponse(jwt);
    }

    public MeResponse getMe(String loginId) {
        var member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        return new MeResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getEmail(),
                member.getRole().name(),
                member.getStatus().name()
        );
    }
}
