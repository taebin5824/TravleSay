package com.taebin.travelsay.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
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

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증된 요청이면 스킵
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("[JwtAuthFilter] path={}", req.getRequestURI());
        }

        // Bearer 토큰 엄격 추출
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
                chain.doFilter(req, res);
                return;
            }

            final String username = jwtTokenProvider.getUsername(token);
            if (username == null || username.isBlank()) {
                log.warn("[JwtAuthFilter] username is null/blank");
                chain.doFilter(req, res);
                return;
            }

            var ud = memberDetailsService.loadUserByUsername(username);
            var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);

            if (log.isDebugEnabled()) log.debug("[JwtAuthFilter] SecurityContext set for user={}", username);
        } catch (Exception e) {
            log.warn("[JwtAuthFilter] validate error: {}", e.getMessage(), e);
            req.setAttribute("jwt_error", e.getMessage());
        }

        chain.doFilter(req, res);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 '엄격하게' 추출한다.
     * - 헤더 이름: "Authorization" / "authorization" / configuredHeaderName 모두 허용
     * - 값: 반드시 "Bearer␠<token>" 형태(대소문자 무시). 공백 없이 붙어 있으면 거부.
     * - token은 JWT 형식(헤더.페이로드.서명)과 URL-safe base64 문자만 허용.
     */
    private String extractBearerToken(HttpServletRequest req, String configuredHeaderName) {
        String header = req.getHeader(AUTHZ_STD);
        if (isBlank(header) && configuredHeaderName != null) header = req.getHeader(configuredHeaderName);
        if (isBlank(header)) header = req.getHeader(AUTHZ_LWR);
        if (isBlank(header)) return null;

        // 앞뒤 공백 제거
        final String h = header.trim();

        // 정확히 첫 번째 공백 위치 찾기 (스킴 + 공백 + 토큰)
        final int spaceIdx = h.indexOf(' ');
        if (spaceIdx <= 0) return null; // 공백이 없거나 " Bearer" 같은 이상한 값

        final String scheme = h.substring(0, spaceIdx).toLowerCase(Locale.ROOT);
        if (!"bearer".equals(scheme)) return null; // 다른 스킴이면 거부

        String token = h.substring(spaceIdx + 1).trim();
        if (isBlank(token)) return null;

        // JWT 빠른 형태 검증: 3개 파트, URL-safe base64 문자만
        if (!token.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")) {
            if (log.isDebugEnabled()) log.debug("[JwtAuthFilter] token format invalid");
            return null;
        }

        // 길이 상한(선택): 과도하게 긴 입력 방어
        if (token.length() > 4096) return null;

        return token;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();

        // 정적/공개 리소스 스킵 (성능 최적화)
        if (p.startsWith("/css/") || p.startsWith("/js/") || p.startsWith("/img/")
                || p.startsWith("/assets/") || p.startsWith("/fragments/")
                || p.equals("/") || p.equals("/index.html") || p.equals("/login.html")
                || p.equals("/favicon.ico")) {
            return true;
        }

        // 인증 불필요 API
        if (p.startsWith("/api/member/signup") || p.startsWith("/api/member/login")
                || p.startsWith("/swagger-ui/") || p.startsWith("/v3/api-docs")
                || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        return false;
    }
}
