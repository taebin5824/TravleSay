document.addEventListener("DOMContentLoaded", () => {
    const form   = document.getElementById("signupForm");
    const $ok    = document.getElementById("signupSuccess");
    const $phone = document.getElementById("phoneNumber");

    // 필드 목록
    const FIELDS = ["loginId", "password", "passwordConfirm", "email", "phoneNumber"];

    // 숫자만
    const onlyDigits = (s) => (s || "").replace(/\D/g, "");

    // 한국형 전화번호 포맷터 (숫자만 받아 포맷)
    function formatPhoneKR(digits) {
        const d = onlyDigits(digits);

        if (d.startsWith("02")) {
            if (d.length <= 2) return d;
            if (d.length <= 5)  return d.replace(/(\d{2})(\d{1,3})/, "$1-$2");
            if (d.length <= 9)  return d.replace(/(\d{2})(\d{3,4})(\d{1,4})/, "$1-$2-$3");
            return d.slice(0,10).replace(/(\d{2})(\d{4})(\d{4})/, "$1-$2-$3");
        }

        if (d.length <= 3)  return d;
        if (d.length <= 7)  return d.replace(/(\d{3})(\d{1,4})/, "$1-$2");
        if (d.length <= 11) return d.replace(/(\d{3})(\d{3,4})(\d{1,4})/, "$1-$2-$3");
        return d.slice(0,11).replace(/(\d{3})(\d{4})(\d{4})/, "$1-$2-$3");
    }

    // 전화번호: 자동 하이픈 + 커서 위치 보정 + 입력 중 에러 해제
    if ($phone) {
        $phone.addEventListener("input", (e) => {
            const input = e.target;

            const before = input.value;
            const pos    = input.selectionStart ?? before.length;

            // 포맷 전 숫자만
            const digitsBefore = onlyDigits(before);
            const formatted = formatPhoneKR(digitsBefore);
            input.value = formatted;

            // 커서 보정: 하이픈 추가/삭제 길이차 반영
            const after = formatted;
            const delta = after.length - before.length;
            const newPos = Math.max(0, Math.min(after.length, (pos ?? after.length) + delta));
            input.selectionStart = input.selectionEnd = newPos;

            // 입력 중이면 전화번호 에러 제거
            setFieldError("phoneNumber", "");
        });
    }

    // 에러 표시/해제(필드별)
    function setFieldError(id, msg) {
        const $msg = document.getElementById(`error-${id}`);
        const $inp = document.getElementById(id);
        if (!$msg || !$inp) return;

        if (msg) {
            $msg.textContent = msg;
            $msg.classList.add("show");
            $inp.classList.add("is-invalid");
        } else {
            $msg.textContent = "";
            $msg.classList.remove("show");
            $inp.classList.remove("is-invalid");
        }
    }

    function clearAllErrors() {
        FIELDS.forEach((f) => setFieldError(f, ""));
    }

    // 입력 중 에러 즉시 해제
    FIELDS.forEach((id) => {
        const $inp = document.getElementById(id);
        if ($inp) {
            $inp.addEventListener("input", () => setFieldError(id, ""));
            $inp.addEventListener("blur",  () => {
                if ($inp.value.trim() !== "") setFieldError(id, "");
            });
        }
    });

    if (!form) return;

    const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    // 엄격한 전화번호 정규식 (하이픈 포함, 02 및 0XX 패턴 허용)
    const phoneRe = /^(02-\d{3,4}-\d{4}|0\d{2}-\d{3,4}-\d{4})$/;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        clearAllErrors();
        if ($ok) $ok.style.display = "none";

        const loginId         = document.getElementById("loginId").value.trim();
        const password        = document.getElementById("password").value;
        const passwordConfirm = document.getElementById("passwordConfirm").value;
        const email           = document.getElementById("email").value.trim();
        const phoneFormatted  = document.getElementById("phoneNumber").value.trim();

        let firstInvalid = null;

        // 필드별 유효성: 해당 필드 아래에만 에러 표시
        if (loginId.length < 4 || loginId.length > 50) {
            setFieldError("loginId", "아이디는 4~50자로 입력해주세요.");
            firstInvalid = firstInvalid || "loginId";
        }
        if (password.length < 8 || password.length > 100) {
            setFieldError("password", "비밀번호는 8~100자로 입력해주세요.");
            firstInvalid = firstInvalid || "password";
        }
        if (passwordConfirm.length < 8 || passwordConfirm.length > 100) {
            setFieldError("passwordConfirm", "비밀번호확인은 8~100자로 입력해주세요.");
            firstInvalid = firstInvalid || "passwordConfirm";
        }
        if (password && passwordConfirm && password !== passwordConfirm) {
            setFieldError("passwordConfirm", "비밀번호가 일치하지 않습니다.");
            firstInvalid = firstInvalid || "passwordConfirm";
        }
        if (!emailRe.test(email)) {
            setFieldError("email", "올바른 이메일을 입력해주세요.");
            firstInvalid = firstInvalid || "email";
        }

        // 전화번호: 필수 + 형식 검사(자동 하이픈 결과 기준)
        if (!phoneFormatted) {
            setFieldError("phoneNumber", "전화번호를 입력해주세요.");
            firstInvalid = firstInvalid || "phoneNumber";
        } else if (!phoneRe.test(phoneFormatted)) {
            setFieldError("phoneNumber", "올바른 형식(예: 010-1234-5678)으로 입력해주세요.");
            firstInvalid = firstInvalid || "phoneNumber";
        }

        if (firstInvalid) {
            document.getElementById(firstInvalid)?.focus();
            return; // 전송 중단
        }

        try {
            const payload = {
                loginId,
                password,
                passwordConfirm,
                email,
                phoneNumber: phoneFormatted
            };

            await axios.post("/api/member/signup", payload, {
                headers: { "Content-Type": "application/json" }
            });

            if ($ok) {
                $ok.textContent = "가입이 완료되었습니다. 로그인 화면으로 이동합니다.";
                $ok.style.display = "block";
            }
            setTimeout(() => (location.href = "/login.html"), 600);
        } catch (err) {
            console.error("회원가입 실패:", err);

            // 서버 에러를 필드별로 매핑 시도
            const status = err.response?.status;
            const data   = err.response?.data;

            if (status === 409) {
                setFieldError("loginId", "이미 사용중인 아이디입니다.");
                document.getElementById("loginId")?.focus();
            } else if (typeof data === "object" && data) {
                // { field: 'phoneNumber', message: '...' } 등 대응
                if (data.field && data.message) {
                    setFieldError(data.field, data.message);
                    document.getElementById(data.field)?.focus();
                } else {
                    // 필드 정보 없으면 이메일 아래에 일반 오류 표시(임시)
                    setFieldError("email", "회원가입 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                    document.getElementById("email")?.focus();
                }
            } else {
                setFieldError("email", String(data || "회원가입 중 오류가 발생했습니다."));
                document.getElementById("email")?.focus();
            }
        }
    });
});
