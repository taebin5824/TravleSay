// ===== 설정 =====
const TOKEN_KEY = "accessToken"
const getToken = () => sessionStorage.getItem(TOKEN_KEY)

// 쿼리스트링에서 id 추출  (/plan-detail.html?id=11)
const planId = Number(new URLSearchParams(location.search).get("id"))

const API_DETAIL = (id) => `/api/trips/plans/${id}/detail` // 통합 상세 API
const API_DAYS = (id) => `/api/trips/plans/${id}/days` // 폴백용
const API_ITEMS = (dayId) => `/api/trips/days/${dayId}/items`
const API_DELETE = (id) => `/api/trips/plans/${id}`
const EDIT_URL = (id) => `/plan-edit.html?id=${id}` // 수정 페이지로 이동

// ===== 공통 fetch (Bearer 자동) =====
async function apiFetch(url, options = {}) {
    const token = getToken()
    if (!token) {
        location.href = "/login.html"
        throw new Error("no token")
    }
    const headers = new Headers(options.headers || {})
    headers.set("Authorization", `Bearer ${token}`)
    const res = await fetch(url, { ...options, headers })
    if (res.status === 401) {
        sessionStorage.removeItem(TOKEN_KEY)
        location.href = "/login.html"
    }
    return res
}

// ===== 엘리먼트 =====
const $title = document.getElementById("planTitle")
const $badges = document.getElementById("planBadges")
const $days = document.getElementById("daysContainer")
const $btnDel = document.getElementById("btnDelete")
const $btnUpd = document.getElementById("btnUpdate") // ✅ 선언된 변수명 사용
const $btnBack = document.getElementById("btnBackToList")
const $textModal = document.getElementById("textModal")
const $modalText = document.getElementById("modalText")

// ===== 유틸 =====
const escapeHtml = (s) =>
    String(s ?? "").replace(
        /[&<>"']/g,
        (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[m],
    )
const fmtDate = (iso) => {
    if (!iso) return "-"
    const d = new Date(iso)
    const y = d.getFullYear(),
        m = String(d.getMonth() + 1).padStart(2, "0"),
        day = String(d.getDate()).padStart(2, "0")
    return `${y}-${m}-${day}`
}
const fmtTime = (hhmmss) => (hhmmss ? hhmmss.slice(0, 5) : "")
const fmtMoney = (n) => (n ?? 0).toLocaleString("ko-KR")

function truncateText(text, maxLength = 20) {
    if (!text || text.length <= maxLength) return escapeHtml(text)
    return `<span class="truncated-text" data-full-text="${escapeHtml(text)}" title="클릭하여 전체 내용 보기">${escapeHtml(text.substring(0, maxLength))}...</span>`
}

function showFullText(text) {
    $modalText.textContent = text
    $textModal.style.display = "flex"
    document.body.style.overflow = "hidden"
}

function closeTextModal() {
    $textModal.style.display = "none"
    document.body.style.overflow = "auto"
}

// Close modal on Escape key
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        closeTextModal()
    }
})

// 시간 범위: 현재 아이템 시작~다음 아이템 시작 (마지막은 "~ —")
function timeRange(items, idx) {
    const cur = items[idx]
    const next = items[idx + 1]
    const a = fmtTime(cur.startTime)
    const b = next && next.startTime ? fmtTime(next.startTime) : "—"
    return a && b ? `${a} ~ ${b}` : a || ""
}

// ===== 렌더 =====
function renderHeader({ title, isPublic, isCompleted }) {
    $title.textContent = title || "계획 상세"
    const parts = []
    parts.push(`<span class="badge ${isPublic ? "public" : "private"}">${isPublic ? "공개" : "비공개"}</span>`)
    if (isCompleted) parts.push(`<span class="badge done">완료</span>`)
    $badges.innerHTML = parts.join("")
}

function renderDays(days) {
    if (!days || !days.length) {
        $days.innerHTML = `<div class="muted">아직 일정이 없습니다.</div>`
        return
    }
    const html = days
        .map((d) => {
            const rows = (d.items || [])
                .map(
                    (it, idx, arr) => `
      <tr>
        <td class="col-time">${timeRange(arr, idx)}</td>
        <td>${truncateText(it.title, 12)}</td>
        <td class="col-amount">${it.amount != null ? truncateText(fmtMoney(it.amount), 15) : ""}</td>
        <td class="col-merchant">${truncateText(it.merchant, 10)}</td>
        <td class="col-memo">${truncateText(it.memo, 8)}</td>
      </tr>
    `,
                )
                .join("")
            return `
      <article class="day-card">
        <div class="day-title">${fmtDate(d.tripDate)}</div>
        <table class="day-table">
          <thead>
            <tr>
              <th class="col-time">시간</th>
              <th>일정명</th>
              <th class="col-amount">사용예정금액</th>
              <th class="col-merchant">사용처</th>
              <th class="col-memo">메모</th>
            </tr>
          </thead>
          <tbody>
            ${rows || `<tr><td colspan="5" class="muted">아이템이 없습니다.</td></tr>`}
          </tbody>
        </table>
      </article>
    `
        })
        .join("")
    $days.innerHTML = html

    $days.addEventListener("click", (e) => {
        if (e.target.classList.contains("truncated-text")) {
            const fullText = e.target.getAttribute("data-full-text")
            if (fullText) {
                showFullText(fullText)
            }
        }
    })
}

// ===== 데이터 로딩 =====
async function loadDetailViaSingleApi(id) {
    const res = await apiFetch(API_DETAIL(id))
    if (res.ok) return res.json()
    if (res.status === 404) throw new Error("not found")
    throw new Error("detail api error")
}

async function loadDetailViaTwoSteps(id) {
    const p1 = await apiFetch(API_DAYS(id))
    if (!p1.ok) throw new Error("days api error")
    const days = await p1.json()
    const filled = await Promise.all(
        days.map(async (d) => {
            const r = await apiFetch(API_ITEMS(d.id ?? d.dayId)) // id/dayId 둘 다 대응
            const items = r.ok ? await r.json() : []
            return { tripDate: d.tripDate, items }
        }),
    )
    return {
        planId: id,
        title: new URLSearchParams(location.search).get("t") || "계획 상세",
        isPublic: true,
        isCompleted: false,
        days: filled,
    }
}

async function loadAndRender() {
    if (!planId) {
        alert("잘못된 접근입니다.")
        return
    }
    try {
        let data
        try {
            data = await loadDetailViaSingleApi(planId)
        } catch {
            data = await loadDetailViaTwoSteps(planId)
        }
        renderHeader(data)
        renderDays(data.days || [])
    } catch (e) {
        console.error(e)
        alert("상세 정보를 불러오지 못했습니다.")
    }
}

// ===== 액션 =====
$btnUpd.addEventListener("click", () => {
    if (!planId) return
    location.href = EDIT_URL(planId)
})

$btnDel.addEventListener("click", async () => {
    if (!planId) return
    if (!confirm("정말 삭제하시겠어요? 이 작업은 되돌릴 수 없습니다.")) return
    try {
        const res = await apiFetch(API_DELETE(planId), { method: "DELETE" })
        if (!res.ok) throw new Error("delete failed")
        alert("삭제되었습니다.")
        location.href = "/my-plans.html"
    } catch (e) {
        console.error(e)
        alert("삭제 중 오류가 발생했습니다.")
    }
})

$btnBack.addEventListener("click", () => {
    location.href = "/my-plans.html"
})

// Close modal on clicking outside
$textModal.addEventListener("click", (e) => {
    if (e.target === $textModal) {
        closeTextModal()
    }
})

loadAndRender()
