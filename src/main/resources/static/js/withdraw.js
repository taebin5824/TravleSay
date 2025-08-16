axios.defaults.baseURL = window.API_BASE_URL || "http://127.0.0.1:8657";
axios.defaults.withCredentials = false;

const form = document.getElementById("withdrawForm");
const $pw = document.getElementById("password");
const $pc = document.getElementById("passwordConfirm");
const $agree = document.getElementById("agree");
const $btn = document.getElementById("btnWithdraw");

const $errPw = document.getElementById("err-password");
const $errPc = document.getElementById("err-passwordConfirm");
const $errAgree = document.getElementById("err-agree");
const $errForm = document.getElementById("err-form");

// === helpers: 표시/숨김 + 인풋 에러 표시 ===
const show = (el, on = true) => el && el.classList.toggle("show", on);
const invalid = (inp, on = true) => inp && inp.classList.toggle("is-invalid", on);

function authHeader() {
    const token = sessionStorage.getItem("accessToken");
    return token ? { Authorization: `Bearer ${token}` } : {};
}

function clearErrors() {
    [$errPw, $errPc, $errAgree, $errForm].forEach(e => { if (e) { e.textContent = ""; e.classList.remove("show"); }});
    [$pw, $pc].forEach(i => i && i.classList.remove("is-invalid"));
}

// === 즉시-검증: 입력 시 에러 제거 ===
$pw?.addEventListener("input", () => { if ($pw.value.trim()) { $errPw.textContent = ""; show($errPw, false); invalid($pw, false); }});
$pc?.addEventListener("input", () => { if ($pw.value === $pc.value) { $errPc.textContent = ""; show($errPc, false); invalid($pc, false); }});
$agree?.addEventListener("change", () => { if ($agree.checked) { $errAgree.textContent = ""; show($errAgree, false); }});

form.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearErrors();

    let ok = true;
    if (!$pw.value.trim()) { $errPw.textContent = "비밀번호를 입력해주세요."; show($errPw, true); invalid($pw, true); ok = false; }
    if ($pw.value !== $pc.value) { $errPc.textContent = "비밀번호가 일치하지 않습니다."; show($errPc, true); invalid($pc, true); ok = false; }
    if (!$agree.checked) { $errAgree.textContent = "탈퇴 안내에 동의해야 진행됩니다."; show($errAgree, true); ok = false; }

    if (!ok) return;

    const payload = { password: $pw.value, passwordConfirm: $pc.value, agreed: true };

    try {
        $btn.disabled = true;
        await axios.post("/api/member/withdraw", payload, { headers: authHeader() });

        // 성공 → 토큰 삭제 후 로그인 페이지로
        sessionStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        location.replace("/login.html");
    } catch (err) {
        const status = err?.response?.status;
        const msg = err?.response?.data?.message || "처리할 수 없습니다. 다시 시도해주세요.";

        if (status === 400) {
            if (/비밀번호.*확인.*일치/.test(msg) || /비밀번호가 일치하지/.test(msg)) {
                $errPc.textContent = "비밀번호가 일치하지 않습니다.";
                show($errPc, true); invalid($pc, true);
            } else if (/동의/.test(msg)) {
                $errAgree.textContent = "탈퇴 안내에 동의해야 진행됩니다.";
                show($errAgree, true);
            } else {
                $errForm.textContent = msg; show($errForm, true);
            }
        } else if (status === 401 || status === 403) {
            sessionStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            location.replace("/login.html");
            return;
        } else {
            $errForm.textContent = msg; show($errForm, true);
        }
    } finally {
        $btn.disabled = false;
    }
});

// 진입 가드
if (!sessionStorage.getItem("accessToken")) {
    location.replace("/login.html");
}
