package com.taebin.travelsay.security;

import com.taebin.travelsay.domain.member.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHZ_STD = "Authorization";
    private static final String AUTHZ_LWR = "authorization";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증된 경우 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        if (log.isDebugEnabled()) log.debug("[JwtAuthFilter] path={}", req.getRequestURI());

        // Bearer 토큰 추출
        final String token = extractBearerToken(req, jwtTokenProvider.headerName());
        if (token == null) {
            if (log.isDebugEnabled()) log.debug("[JwtAuthFilter] no valid Bearer token");
            chain.doFilter(req, res);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("[JwtAuthFilter] token.head='{}...'", token.substring(0, Math.min(10, token.length())));
        }

        try {
            if (!jwtTokenProvider.validate(token)) {
                log.warn("[JwtAuthFilter] token validate = false");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            final String username = jwtTokenProvider.getUsername(token);
            final Integer verInToken = jwtTokenProvider.getVersion(token);
            if (username == null || username.isBlank() || verInToken == null) {
                log.warn("[JwtAuthFilter] username/ver invalid (username='{}', ver={})", username, verInToken);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // DB tokenVersion과 비교
            final Integer currentVer = memberRepository.findTokenVersionByLoginId(username);
            if (currentVer == null || !verInToken.equals(currentVer)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 인증객체 구성 (details에는 tv만 담는다)
            var ud = memberDetailsService.loadUserByUsername(username);
            var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());

            auth.setDetails(verInToken);
            req.setAttribute("jwtVersion", verInToken);

            SecurityContextHolder.getContext().setAuthentication(auth);
            if (log.isDebugEnabled()) log.debug("[JwtAuthFilter] SecurityContext set user={}, tv={}", username, verInToken);
        } catch (Exception e) {
            log.warn("[JwtAuthFilter] validate error: {}", e.getMessage(), e);
            req.setAttribute("jwt_error", e.getMessage());
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(req, res);
    }

    private String extractBearerToken(HttpServletRequest req, String configuredHeaderName) {
        String header = req.getHeader(AUTHZ_STD);
        if (isBlank(header) && configuredHeaderName != null) header = req.getHeader(configuredHeaderName);
        if (isBlank(header)) header = req.getHeader(AUTHZ_LWR);
        if (isBlank(header)) return null;

        final String h = header.trim();
        final int spaceIdx = h.indexOf(' ');
        if (spaceIdx <= 0) return null;

        final String scheme = h.substring(0, spaceIdx).toLowerCase(Locale.ROOT);
        if (!"bearer".equals(scheme)) return null;

        String token = h.substring(spaceIdx + 1).trim();
        if (isBlank(token)) return null;

        if (!token.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")) return null;
        if (token.length() > 4096) return null;

        return token;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        final String p = req.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        // 정적/공개 리소스
        if (p.startsWith("/css/") || p.startsWith("/js/") || p.startsWith("/img/")
                || p.startsWith("/assets/") || p.startsWith("/fragments/")
                || p.equals("/favicon.ico")) return true;

        // 공개 페이지
        if (p.equals("/") || p.equals("/index.html") || p.equals("/login.html")
                || p.equals("/signup.html") || p.equals("/profile-edit.html")
                || p.equals("/withdraw.html") || p.equals("/my-plans.html")
                || p.equals("/plan-edit.html") || p.equals("/plan-detail.html")) return true;

        // 공개 API
        if (p.equals("/api/member/signup") || p.equals("/api/member/login")) return true;

        if (p.startsWith("/swagger-ui/") || p.startsWith("/v3/api-docs")) return true;

        return false;
    }
}
