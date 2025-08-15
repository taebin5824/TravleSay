document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("loginForm");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const loginId  = document.getElementById("loginId").value.trim();
        const password = document.getElementById("password").value;

        if (!loginId || !password) {
            alert("아이디와 비밀번호를 모두 입력해주세요.");
            return;
        }

        try {
            // 서버에 로그인 요청
            const res = await axios.post(
                "/api/member/login",
                {
                    loginId: loginId,
                    password: password
                },
            {
                    headers: { "Content-Type": "application/json" }
                }
            );

            // 서버 응답: accessToken만 있다고 가정
            let { accessToken } = res.data;
            if (!accessToken) throw new Error("accessToken이 응답에 없습니다.");

            accessToken = accessToken.replace(/^Bearer\s+/i, "");

            // accessToken만 저장 (세션 스토리지)
            sessionStorage.setItem("accessToken", accessToken);

            // 메인으로 이동
            location.href = "/index.html";
        } catch (err) {
            console.error("로그인 실패:", err);
            if (err.response?.status === 401) {
                alert("아이디 또는 비밀번호가 올바르지 않습니다.");
            } else {
                alert("로그인 중 오류가 발생했습니다.");
            }
        }
    });
});
