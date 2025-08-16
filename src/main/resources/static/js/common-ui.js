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
        return true;
    }
}
function isLoggedIn() {
    const t = getAccessToken();
    return !!t && !isTokenExpired(t);
}

async function includeFragment(el) {
    const url = el.getAttribute("data-include");
    if (!url) return;
    const res = await fetch(url, { cache: "no-cache" });
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    el.innerHTML = await res.text();
    if (el.id === "__header__") {
        await renderAuth();
        // ✅ 이벤트 위임: 한 번만 등록
        setupAuthDelegation();
    }
}

async function includeAll() {
    const targets = document.querySelectorAll("[data-include]");
    for (const el of targets) {
        try { await includeFragment(el); }
        catch (e) { console.error("include error:", el.getAttribute("data-include"), e); }
    }
}

async function renderAuth() {
    const box = document.getElementById("authSection");
    if (!box) {
        console.warn("[auth] #authSection 없음");
        return;
    }

    if (!isLoggedIn()) {
        box.innerHTML = `<a href="/login.html" class="login-link">로그인</a>`;
        console.debug("[auth] not logged in → show login link");
        return;
    }

    const token = getAccessToken();
    try {
        const res = await axios.get("/api/member/me", {
            headers: { Authorization: `Bearer ${token}` }
        });
        const { loginId } = res.data || {};
        if (!loginId) {
            box.innerHTML = `<a href="/login.html" class="login-link">로그인</a>`;
            console.debug("[auth] /me ok but no loginId → show login link");
            return;
        }

        box.innerHTML = `
  <button class="btn" id="btnLogout" type="button">로그아웃</button>
  <a class="avatar" id="navAvatar" href="/profile-edit.html" title="개인정보 수정">
    <img src="https://i.pravatar.cc/48?u=${encodeURIComponent(loginId)}" alt="">
    <span id="navLoginId">${loginId}</span>
  </a>
`;
        console.debug("[auth] logged in as:", loginId);

    } catch (err) {
        console.error("[auth] /api/member/me 실패:", err);
        box.innerHTML = `<a href="/login.html" class="login-link">로그인</a>`;
    }
}

let _authDelegationBound = false;
function setupAuthDelegation() {
    if (_authDelegationBound) return;
    const box = document.getElementById("authSection");
    if (!box) return;

    box.addEventListener("click", async (e) => {
        const target = e.target.closest("#btnLogout");
        if (!target) return;
        e.preventDefault();

        const token = getAccessToken();
        console.debug("[logout] clicked, token exists?", !!token);

        try {
            if (token) {
                const res = await axios.post("/api/member/logout", null, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                console.debug("[logout] server ok:", res.status);
            } else {
                console.debug("[logout] no token, skipping server call");
            }
        } catch (err) {
            console.warn("[logout] API 실패(프론트만 삭제):", err);
        } finally {
            sessionStorage.removeItem("accessToken");
            location.href = "/index.html";
        }
    });

    _authDelegationBound = true;
    console.debug("[auth] delegation bound");
}

document.addEventListener("DOMContentLoaded", () => {
    includeAll();
});
