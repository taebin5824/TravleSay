package com.taebin.travelsay.service;


import com.taebin.travelsay.domain.member.Member;
import com.taebin.travelsay.domain.member.MemberRepository;
import com.taebin.travelsay.domain.member.MemberRole;
import com.taebin.travelsay.domain.member.MemberStatus;
import com.taebin.travelsay.dto.member.request.InactivateMemberRequest;
import com.taebin.travelsay.dto.member.request.LoginRequest;
import com.taebin.travelsay.dto.member.request.SignupRequest;
import com.taebin.travelsay.dto.member.request.UpdateProfileRequest;
import com.taebin.travelsay.dto.member.response.AuthResponse;
import com.taebin.travelsay.dto.member.response.MeResponse;
import com.taebin.travelsay.dto.member.response.UpdateProfileResponse;
import com.taebin.travelsay.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다");
        }

        var existingOpt = memberRepository.findByLoginId(request.loginId());

        if (existingOpt.isPresent()) {
            Member existing = existingOpt.get();

            // 이미 ACTIVE면 중복 가입 불가
            if (existing.getStatus() == MemberStatus.ACTIVE) {
                throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
            }

            // INACTIVE면 재가입 처리,  본인 제외 다른 사용자와 이메일/전화번호 중복만 막음
            if (memberRepository.existsByEmailAndMemberIdNot(request.email(), existing.getMemberId())) {
                throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
            }
            if (memberRepository.existsByPhoneNumberAndMemberIdNot(request.phoneNumber(), existing.getMemberId())) {
                throw new IllegalArgumentException("이미 사용중인 전화번호입니다.");
            }

            String encoded = passwordEncoder.encode(request.password());

            // 재활성화 및 최신정보 갱신
            existing.setPassword(encoded);
            existing.setEmail(request.email());
            existing.setPhoneNumber(request.phoneNumber());
            existing.setStatus(MemberStatus.ACTIVE);

            // 기존 토큰 무효화
            existing.setTokenVersion(existing.getTokenVersion() + 1);

            return;
        }

        // 신규 가입, 이메일/전화번호는 전체 회원 기준 중복체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }
        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new IllegalArgumentException("이미 사용중인 전화번호입니다.");
        }

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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.loginId(), request.password());
        authenticationManager.authenticate(authToken);

        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        String accessToken = jwtTokenProvider.createToken(
                member.getMemberId(),
                member.getLoginId(),
                member.getTokenVersion()
        );

        return new AuthResponse(accessToken);
    }

    @Transactional
    public void logoutByMemberId(String memberId, Integer verInToken) {
        if (memberId == null || memberId.isBlank()) return;

        if (verInToken != null) {
            memberRepository.bumpIfMatchesByMemberId(memberId, verInToken);
        }
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(String loginId) {
        var member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        return new MeResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getEmail(),
                member.getPhoneNumber(),
                member.getRole().name(),
                member.getStatus().name()
        );
    }

    @Transactional
    public UpdateProfileResponse updateProfile(String currentLoginId, UpdateProfileRequest req) {
        Member m = memberRepository.findByLoginId(currentLoginId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 요청값 (비번은 trim 금지)
        final String curPw   = req.currentPassword();
        final String newId   = safeTrim(req.loginId());
        final String newMail = safeTrim(req.email());
        final String newTel  = safeTrim(req.phoneNumber());
        final String newPw   = req.password();
        final String newPwC  = req.passwordConfirm();

        // 변경 여부 계산
        final boolean idChanged   = newId   != null && !newId.isBlank()   && !newId.equals(m.getLoginId());
        final boolean mailChanged = newMail != null && !newMail.isBlank() && !newMail.equals(m.getEmail());
        final boolean telChanged  = newTel  != null && !newTel.isBlank()  && !newTel.equals(m.getPhoneNumber());

        final boolean pwGiven     = newPw   != null && !newPw.isBlank();
        final boolean pwCGiven    = newPwC  != null && !newPwC.isBlank();
        final boolean pwRequested = pwGiven || pwCGiven;
        final boolean pwReady     = pwGiven && pwCGiven;

        if (!idChanged && !mailChanged && !telChanged && !pwRequested) {
            throw new IllegalArgumentException("변경사항이 없습니다.");
        }

        // 어떤 변경이든 현재 비밀번호 필수
        if (curPw == null || curPw.isBlank() || !passwordEncoder.matches(curPw, m.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        boolean reloginRequired = false;


        if (idChanged) {
            if (memberRepository.existsByLoginId(newId)) {
                throw new IllegalArgumentException("이미 사용중인 아이디입니다.");
            }
            m.setLoginId(newId);
            reloginRequired = true;
        }
        if (mailChanged) {
            if (memberRepository.existsByEmail(newMail)) {
                throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
            }
            m.setEmail(newMail);
        }
        if (telChanged) {
            if (memberRepository.existsByPhoneNumber(newTel)) {
                throw new IllegalArgumentException("이미 사용중인 전화번호입니다.");
            }
            m.setPhoneNumber(newTel);
        }

        if (pwRequested) {
            if (!pwReady) {
                throw new IllegalArgumentException("비밀번호와 비밀번호 확인을 모두 입력해 주세요.");
            }
            if (!newPw.equals(newPwC)) {
                throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            }
            if (passwordEncoder.matches(newPw, m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호와 다른 비밀번호를 사용해 주세요.");
            }
            m.setPassword(passwordEncoder.encode(newPw));
            reloginRequired = true;
        }

        // 아이디, 비밀번호 변경 시 토큰 버전 증가
        if (reloginRequired) {
            m.setTokenVersion(m.getTokenVersion() + 1);
        }

        return new UpdateProfileResponse(reloginRequired);
    }


    @Transactional
    public void inactivateMember(String memberId, InactivateMemberRequest request) {
        if (!request.agreed()) {
            throw new IllegalArgumentException("탈퇴 안내에 동의해주세요.");
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (member.getStatus() == MemberStatus.INACTIVE) {
            return;
        }

        member.setStatus(MemberStatus.INACTIVE);
        member.setTokenVersion(member.getTokenVersion() + 1);
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
