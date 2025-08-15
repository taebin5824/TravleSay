function getAccessToken() {
    try { return sessionStorage.getItem("accessToken"); } catch { return null; }
}
function isTokenExpired(token) {
    if (!token) return true;
    try {
        const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
        const expMs = (payload.exp || 0) * 1000;
        return Date.now() >= expMs;
    } catch {
        return true; // 파싱 실패 시 만료로 간주
    }
}
function isLoggedIn() {
    const t = getAccessToken();
    return !!t && !isTokenExpired(t);
}

// =============== 헤더/푸터 인클루드 ===============
async function includeFragment(el) {
    const url = el.getAttribute("data-include");
    if (!url) return;
    const res = await fetch(url, { cache: "no-cache" });
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    el.innerHTML = await res.text();
    // 헤더가 갓 삽입된 경우 로그인 영역 렌더링
    if (el.id === "__header__") {
        await renderAuth();
    }
}

async function includeAll() {
    const targets = document.querySelectorAll("[data-include]");
    for (const el of targets) {
        try { await includeFragment(el); }
        catch (e) { console.error("include error:", el.getAttribute("data-include"), e); }
    }
}

// =============== 인증 영역 렌더링 (loginId 표기) ===============
async function renderAuth() {
    const box = document.getElementById("authSection");
    if (!box) return;

    // 로그인 안됨 → 로그인 링크
    if (!isLoggedIn()) {
        box.innerHTML = `<a href="/login.html" class="login-link">로그인</a>`;
        return;
    }

    // 로그인됨 → /api/member/me 호출해서 loginId 표시
    const token = getAccessToken();
    try {
        const res = await axios.get("/api/member/me", {
            headers: { Authorization: `Bearer ${token}` }
        });
        const { loginId } = res.data || {};

        // loginId가 없으면 안전하게 로그인 링크로 대체
        if (!loginId) {
            box.innerHTML = `<a href="/login.html" class="login-link">로그인</a>`;
            return;
        }

        box.innerHTML = `
      <button class="btn" id="btnLogout" type="button">로그아웃</button>
      <div class="avatar">
        <img src="https://i.pravatar.cc/48?u=${encodeURIComponent(loginId)}" alt="">
        <span>${loginId}</span>
      </div>
    `;

        // 로그아웃 클릭 시 토큰 제거 후 메인 이동
        document.getElementById("btnLogout")?.addEventListener("click", () => {
            sessionStorage.removeItem("accessToken");
            location.href = "/index.html";
        });

    } catch (err) {
        console.error("사용자 정보 불러오기 실패:", err);
        // 401 등 실패 시 로그인 링크로 전환
        box.innerHTML = `<a href="/login.html" class="login-link">로그인</a>`;
    }
}

// =============== 부트스트랩 ===============
document.addEventListener("DOMContentLoaded", () => {
    includeAll();
});
