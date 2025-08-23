// ====== 설정 ======
const API_ALL = "/api/trips/plans/my"
const PAGE_SIZE = 10
const PAGE_WINDOW = 5
const DETAIL_URL = (id) => `/plan-detail.html?id=${id}`
const CREATE_URL = "plan-edit.html"

// ====== 토큰 헬퍼 ======
const TOKEN_KEY = "accessToken" // 로그인 성공 시 여기에 저장해 둔다
const getToken = () => sessionStorage.getItem(TOKEN_KEY)

// 모든 API 호출을 감싸 Bearer 헤더를 자동 추가
async function apiFetch(url, options = {}) {
    const token = getToken()
    if (!token) {
        // 토큰이 없으면 곧바로 로그인으로
        location.href = "/login.html"
        return new Response(null, { status: 401 })
    }
    const headers = new Headers(options.headers || {})
    headers.set("Authorization", `Bearer ${token}`)

    // Bearer만 쓰면 쿠키 보낼 필요 없음 → credentials 생략/omit
    const res = await fetch(url, { ...options, headers /*, credentials: 'omit' */ })

    if (res.status === 401) {
        // 만료/유효하지 않은 토큰
        sessionStorage.removeItem(TOKEN_KEY)
        location.href = "/login.html"
    }
    return res
}

// ====== 엘리먼트 ======
const $list = document.getElementById("planList")
const $pagination = document.getElementById("pagination")
const $empty = document.getElementById("emptyState")
const $create = document.getElementById("createBtn")
$create.addEventListener("click", () => (location.href = CREATE_URL))

// ====== 상태 ======
const state = { rows: [], page: 0 }

// ====== 유틸 ======
function escapeHtml(s) {
    return String(s ?? "").replace(
        /[&<>"']/g,
        (m) =>
            ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;",
            })[m],
    )
}
function fmtDate(iso) {
    if (!iso) return "-"
    const d = new Date(iso)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, "0")
    const day = String(d.getDate()).padStart(2, "0")
    return `${y}-${m}-${day}`
}
function isPast(iso) {
    if (!iso) return false
    const d = new Date(iso)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return d < today
}

// ====== 렌더 ======
function renderList(rows) {
    if (!rows.length) {
        $list.innerHTML = ""
        $empty.style.display = "block"
        return
    }
    $empty.style.display = "none"
    $list.innerHTML = rows
        .map((r) => {
            let dateClass = ""
            let statusText = ""

            if (r.isCompleted) {
                dateClass = "completed"
                statusText = " (완료)"
            } else if (isPast(r.startDate)) {
                dateClass = "past"
                statusText = " (기간 만료)"
            }

            return `
        <li class="plan-row" data-id="${r.planId}">
          <div class="date ${dateClass}">
            ${fmtDate(r.startDate)}${statusText}
          </div>
          <div class="title" title="${escapeHtml(r.title)}">${escapeHtml(r.title)}</div>
          <div class="pub">${r.isPublic ? "공개" : "비공개"}</div>
        </li>
      `
        })
        .join("")
}
$list.addEventListener("click", (e) => {
    const row = e.target.closest(".plan-row")
    if (!row) return
    const id = row.getAttribute("data-id")
    if (id) location.href = DETAIL_URL(id)
})

function renderPagination(totalPages, cur0) {
    if (totalPages <= 1) {
        $pagination.innerHTML = ""
        return
    }
    const cur = cur0 + 1
    const total = totalPages
    const half = Math.floor(PAGE_WINDOW / 2)
    let start = Math.max(1, cur - half)
    const end = Math.min(total, start + PAGE_WINDOW - 1)
    if (end - start + 1 < PAGE_WINDOW) start = Math.max(1, end - PAGE_WINDOW + 1)

    const parts = []
    parts.push(btn(cur0 - 1, "«", cur === 1))

    if (start > 1) {
        parts.push(btn(0, "1", cur === 1))
        if (start > 2) parts.push(ellipsis())
    }
    for (let p = start; p <= end; p++) parts.push(btn(p - 1, String(p), p === cur))
    if (end < total) {
        if (end < total - 1) parts.push(ellipsis())
        parts.push(btn(total - 1, String(total), cur === total))
    }
    parts.push(btn(cur0 + 1, "»", cur === total))

    $pagination.innerHTML = parts.join("")
    $pagination.querySelectorAll(".page-btn[data-page]").forEach((el) => {
        el.addEventListener("click", () => renderPage(Number.parseInt(el.dataset.page, 10)))
    })

    function btn(page0, label, disabled) {
        return `<button class="page-btn ${disabled ? "disabled" : ""} ${!disabled && state.page === page0 ? "active" : ""}" ${disabled ? "disabled" : ""} data-page="${page0}">${label}</button>`
    }
    function ellipsis() {
        return `<span class="dots">…</span>`
    }
}

function renderPage(page0) {
    state.page = Math.max(0, page0)
    const totalPages = Math.max(1, Math.ceil(state.rows.length / PAGE_SIZE))
    const start = state.page * PAGE_SIZE
    const slice = state.rows.slice(start, start + PAGE_SIZE)
    renderList(slice)
    renderPagination(totalPages, state.page)
}

// ====== 로드 ======
async function loadAll() {
    try {
        const res = await apiFetch(API_ALL) // << Bearer 자동 첨부
        if (!res.ok) throw new Error("API 오류")
        state.rows = await res.json()
        renderPage(0)
    } catch (e) {
        console.error(e)
        alert("목록을 불러오지 못했습니다.")
    }
}
loadAll()
