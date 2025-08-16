/* 개인정보수정 페이지 전용 JS
   - /api/member/me로 초기값 채우기 (아이디/이메일/전화번호)
   - 변경된 것만 서버에 전송
   - 항상 현재 비밀번호 모달 표시 → 확인 후 전송
   - 에러는 각 입력 아래 .error-msg에만 표시(평소엔 숨김)
*/

(function () {
    // ====== DOM & 유틸 ======
    const $  = (s) => document.querySelector(s);
    const $$ = (s) => document.querySelectorAll(s);
    const getAccessToken = () => { try { return sessionStorage.getItem("accessToken"); } catch { return null; } };
    const hasText = (v) => typeof v === "string" && v.trim().length > 0;

    const form = $("#profileForm");
    const btnSubmit = $("#btnSubmit");

    const loginIdEl = $("#loginId");
    const passwordEl = $("#password");
    const passwordConfirmEl = $("#passwordConfirm");
    const emailEl = $("#email");
    const phoneEl = $("#phoneNumber");

    // 모달
    const modal = $("#pwModal");
    const currentPwEl = $("#currentPassword");
    const btnPwCancel = $("#btnPwCancel");
    const btnPwConfirm = $("#btnPwConfirm");

    // 상태 플래그
    let pageLoaded = false; // /me 로드 완료
    let idDirty = false;    // 아이디 입력 직접 수정 여부
    let pwDirty = false;    // 비밀번호/확인 입력 직접 수정 여부

    // 초기값
    let initial = { loginId: "", email: "", phoneNumber: "" };

    // 버튼은 로드 전 비활성화(HTML에서 disabled가 없다면 여기서 강제)
    if (btnSubmit) btnSubmit.disabled = true;

    // 에러 helpers
    function showFieldError(id, msg) {
        const input = $(`#${id}`);
        const box = $(`#err-${id}`);
        if (input) input.classList.add("is-invalid");
        if (box) { box.textContent = msg || ""; box.classList.add("show"); }
    }
    function clearFieldError(id) {
        const input = $(`#${id}`);
        const box = $(`#err-${id}`);
        if (input) input.classList.remove("is-invalid");
        if (box) { box.textContent = ""; box.classList.remove("show"); }
    }
    function clearAllErrors() {
        ["loginId","password","passwordConfirm","email","phoneNumber","currentPassword"].forEach(clearFieldError);
        const g = $("#err-form"); if (g) { g.textContent = ""; g.classList.remove("show"); }
    }
    function showFormError(msg) {
        const g = $("#err-form"); if (g) { g.textContent = msg || ""; g.classList.add("show"); }
    }

    function mapServerErrorToField(message) {
        if (!message) return null;
        if (message.includes("현재 비밀번호")) return "currentPassword";
        if (message.includes("비밀번호 확인")) return "passwordConfirm";
        if (message.includes("비밀번호")) return "password";
        if (message.includes("아이디")) return "loginId";
        if (message.includes("이메일")) return "email";
        if (message.includes("전화번호")) return "phoneNumber";
        return null;
    }

    // ====== 초기 데이터 로드 ======
    async function loadMe() {
        const token = getAccessToken();
        if (!token) { location.href = "/login.html"; return; }
        try {
            const res = await axios.get("/api/member/me", {
                headers: { Authorization: `Bearer ${token}` }
            });
            const me = res.data || {};
            initial.loginId = me.loginId || "";
            initial.email = me.email || "";
            initial.phoneNumber = me.phoneNumber || "";

            // 입력값 세팅
            loginIdEl.value = initial.loginId;
            emailEl.value = initial.email;
            phoneEl.value = initial.phoneNumber;

            // 자동완성 오인 방지: 비번 칸은 항상 비우기
            if (passwordEl) passwordEl.value = "";
            if (passwordConfirmEl) passwordConfirmEl.value = "";

            pageLoaded = true;
            if (btnSubmit) btnSubmit.disabled = false;
        } catch (err) {
            const st = err?.response?.status;
            if (st === 401 || st === 403) { location.href = "/login.html"; return; }
            showFormError("사용자 정보를 불러오지 못했습니다.");
        }
    }

    // ====== dirty 플래그 ======
    loginIdEl?.addEventListener("input", () => { idDirty = true; });
    passwordEl?.addEventListener("input", () => { pwDirty = true; });
    passwordConfirmEl?.addEventListener("input", () => { pwDirty = true; });

    // ====== 모달 ARIA/포커스 제어 ======
    let pendingPayload = null;     // 모달 확인 후 보낼 페이로드 임시 저장
    let lastFocusedEl = null;      // 모달 열기 전 포커스 기억
    const pageRoots = [document.querySelector("header"), document.querySelector("main"), document.querySelector("footer")];

    function setPageInert(on) {
        pageRoots.forEach(r => {
            if (!r) return;
            if (on) r.setAttribute("inert", "");
            else r.removeAttribute("inert");
        });
    }

    function getModalFocusable() {
        return modal.querySelectorAll(
            'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])'
        );
    }

    function trapTab(e) {
        if (e.key !== "Tab") return;
        const focusables = Array.from(getModalFocusable()).filter(el => el.offsetParent !== null);
        if (focusables.length === 0) return;
        const first = focusables[0];
        const last  = focusables[focusables.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    }

    function openModal() {
        clearFieldError("currentPassword");
        currentPwEl.value = "";
        lastFocusedEl = document.activeElement;

        setPageInert(true);                          // 배경 비활성화
        modal.style.display = "flex";
        modal.setAttribute("aria-hidden", "false");
        document.addEventListener("keydown", trapTab);
        document.body.style.overflow = "hidden";

        // Safari 대비
        currentPwEl.setAttribute("tabindex", "-1");
        currentPwEl.focus();
    }

    function closeModal() {
        // 포커스를 모달 밖으로 먼저 이동
        (lastFocusedEl ?? btnSubmit)?.focus();

        modal.setAttribute("aria-hidden", "true");
        modal.style.display = "none";
        document.removeEventListener("keydown", trapTab);
        document.body.style.overflow = "";
        setPageInert(false);
        lastFocusedEl = null;
    }

    // Esc로 닫기
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal.style.display !== "none") {
            pendingPayload = null;
            closeModal();
            // ✅ 제출 가능 상태 복구
            btnSubmit.dataset.busy = "0";
        }
    });

    // 모달 버튼
    btnPwCancel?.addEventListener("click", () => {
        pendingPayload = null;
        closeModal();
        // ✅ 제출 가능 상태 복구
        btnSubmit.dataset.busy = "0";
    });

    btnPwConfirm?.addEventListener("click", async () => {
        if (btnPwConfirm.dataset.busy === "1") return;
        btnPwConfirm.dataset.busy = "1";

        clearFieldError("currentPassword");
        const cpw = currentPwEl.value;
        if (!hasText(cpw)) {
            showFieldError("currentPassword", "현재 비밀번호를 입력해 주세요.");
            btnPwConfirm.dataset.busy = "0";
            return;
        }
        if (!pendingPayload) { btnPwConfirm.dataset.busy = "0"; return; }

        pendingPayload.currentPassword = cpw;
        closeModal();
        await submitUpdate(pendingPayload, /*forceRelogin*/ false); // 서버 응답 우선
        pendingPayload = null;
        btnPwConfirm.dataset.busy = "0";
    });

    // Enter로 확인
    currentPwEl?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") btnPwConfirm.click();
    });

    // ====== 제출 처리 (항상 모달을 거침) ======
    form?.addEventListener("submit", async (e) => {
        e.preventDefault();
        if (!pageLoaded) { showFormError("잠시만요. 사용자 정보를 불러오는 중입니다."); return; }
        if (btnSubmit.dataset.busy === "1") return;
        btnSubmit.dataset.busy = "1";

        clearAllErrors();

        const token = getAccessToken();
        if (!token) { location.href = "/login.html"; return; }

        const loginId = (loginIdEl.value || "").trim();
        const password = passwordEl.value || "";              // 비번은 trim하지 않음
        const passwordConfirm = passwordConfirmEl.value || "";
        const email = (emailEl.value || "").trim();
        const phoneNumber = (phoneEl.value || "").trim();

        // 비밀번호 섹션 기본 유효성(직접 입력했을 때만 검사)
        if (pwDirty && (hasText(password) ^ hasText(passwordConfirm))) {
            showFieldError(hasText(password) ? "passwordConfirm" : "password", "비밀번호와 비밀번호 확인을 모두 입력해 주세요.");
            btnSubmit.dataset.busy = "0"; return;
        }
        if (pwDirty && hasText(password) && hasText(passwordConfirm) && password !== passwordConfirm) {
            showFieldError("passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            btnSubmit.dataset.busy = "0"; return;
        }

        // 변경된 것만 payload 구성 (자동완성 오인 방지: dirty 기반)
        const payload = {
            currentPassword: "", // 모달에서만 채움

            // 아이디: 사용자가 수정했고(idDirty) 실제로 바뀐 경우에만 보냄
            loginId: (idDirty && hasText(loginId) && loginId !== initial.loginId) ? loginId : "",

            // 비번: 사용자가 입력(pwDirty)했고 두 칸 모두 채워진 경우에만 보냄
            password: (pwDirty && hasText(password) && hasText(passwordConfirm)) ? password : "",
            passwordConfirm: (pwDirty && hasText(password) && hasText(passwordConfirm)) ? passwordConfirm : "",

            // 비민감: 값이 실제로 바뀐 경우에만
            email: (hasText(email) && email !== initial.email) ? email : "",
            phoneNumber: (hasText(phoneNumber) && phoneNumber !== initial.phoneNumber) ? phoneNumber : ""
        };

        // 변경 없음
        if (!hasText(payload.loginId) && !hasText(payload.password)
            && !hasText(payload.email) && !hasText(payload.phoneNumber)) {
            showFormError("변경사항이 없습니다.");
            btnSubmit.dataset.busy = "0"; return;
        }

        // ✅ 이제는 항상 모달을 통해 현재 비밀번호 확인
        pendingPayload = payload;
        openModal();
        // busy 해제는 모달 취소/ESC 또는 submitUpdate()에서 처리
    });

    async function submitUpdate(payload, forceRelogin) {
        console.debug("[profile-edit] payload", JSON.parse(JSON.stringify(payload)));
        const token = getAccessToken();
        if (!token) { location.href = "/login.html"; return; }

        try {
            const res = await axios.put("/api/member/update", payload, {
                headers: { Authorization: `Bearer ${token}` }
            });

            // 서버가 reloginRequired를 주면 그 값을, 없으면 forceRelogin(기본 false) 사용
            const reloginRequired = res?.data?.reloginRequired ?? !!forceRelogin;

            if (reloginRequired) {
                alert("정보가 변경되어 다시 로그인해 주세요.");
                sessionStorage.removeItem("accessToken");
                location.href = "/login.html";
            } else {
                alert("정보가 수정되었습니다.");
                location.href = "/index.html";
            }
        } catch (err) {
            const st = err?.response?.status;
            if (st === 401) { // 인증 문제는 로그인으로
                sessionStorage.removeItem("accessToken");
                location.href = "/login.html";
                return;
            }
            const msg = err?.response?.data?.message || err?.message || "요청을 처리할 수 없습니다.";
            const field = mapServerErrorToField(msg);
            if (field) showFieldError(field, msg); else showFormError(msg);
        } finally {
            // ✅ 제출 가능 상태 복구
            btnSubmit.dataset.busy = "0";
        }
    }

    // ====== 부트스트랩 ======
    document.addEventListener("DOMContentLoaded", loadMe);
})();
