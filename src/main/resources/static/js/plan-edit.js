// ================== 설정 ==================
const API_PLAN_CREATE = "/api/trips/plans"
const API_PLAN_DETAIL = (id) => `/api/trips/plans/${id}`
const API_PLAN_UPDATE = (id) => `/api/trips/plans/${id}`

const API_DAYS = (id) => `/api/trips/plans/${id}/days`
const API_DAY_CREATE = (id) => `/api/trips/plans/${id}/days`
const API_ITEMS = (dayId) => `/api/trips/days/${dayId}/items`
const API_SHIFT = (dayId) => `/api/trips/days/${dayId}/items/shift-time`

const API_ITEM_CREATE = (dayId) => `/api/trips/days/${dayId}/items`
const API_ITEM_UPDATE = (itemId) => `/api/trips/items/${itemId}`
const API_ITEM_DELETE = (itemId) => `/api/trips/items/${itemId}`
const API_ITEM_REORDER = (itemId) => `/api/trips/items/${itemId}/order`

// ================== 토큰/HTTP 공통 ==================
const TOKEN_KEY = "accessToken"
const getToken = () => sessionStorage.getItem(TOKEN_KEY)

async function apiFetch(url, options = {}) {
    const token = getToken()
    if (!token) {
        location.href = "/login.html"
        return new Response(null, { status: 401 })
    }
    const headers = new Headers(options.headers || {})
    headers.set("Authorization", `Bearer ${token}`)
    headers.set("Accept", "application/json")

    let body = options.body
    if (body && !(body instanceof FormData)) {
        headers.set("Content-Type", "application/json")
        body = JSON.stringify(body)
    }
    const res = await fetch(url, { ...options, headers, body, credentials: "omit" })
    if (res.status === 401) {
        sessionStorage.removeItem(TOKEN_KEY)
        location.href = "/login.html"
    }
    return res
}
async function httpGet(url) {
    const res = await apiFetch(url)
    if (!res.ok) throw await buildError(res)
    return res.status === 204 ? null : res.json()
}
async function httpPost(url, body) {
    const res = await apiFetch(url, { method: "POST", body })
    if (!res.ok) throw await buildError(res)
    return res.status === 204 ? null : res.json()
}
async function httpPatch(url, body) {
    const res = await apiFetch(url, { method: "PATCH", body })
    if (!res.ok) throw await buildError(res)
    return res.status === 204 ? null : res.json()
}
async function httpDelete(url) {
    const res = await apiFetch(url, { method: "DELETE" })
    if (!res.ok) throw await buildError(res)
    return null
}
async function buildError(res) {
    let msg = `HTTP ${res.status}`
    try {
        const j = await res.json()
        if (j?.message) msg = j.message
    } catch {}
    const err = new Error(msg)
    err.status = res.status
    return err
}

// ================== 상태/엘리먼트 ==================
const q = new URLSearchParams(location.search)
const planId = Number(q.get("id"))
const isEdit = Number.isFinite(planId) && planId > 0

const state = {
    plan: { id: null, title: "", isPublic: false, isCompleted: false },
    days: [], // [{id, tripDate}]
    curDayId: null, // 선택된 dayId
    items: [], // 현재 Day 아이템
}

const $title = document.getElementById("planTitle")
const $isPublic = document.getElementById("isPublic")
const $isCompleted = document.getElementById("isCompleted")
const $tabs = document.getElementById("dayTabs")
const $newDayInput = document.getElementById("newDayInput")
const $btnAddDay = document.getElementById("btnAddDay")
const $dayCard = document.getElementById("dayCard")
const $curDayLabel = document.getElementById("curDayLabel")
const $shiftOffset = document.getElementById("shiftOffset")
const $btnShift = document.getElementById("btnShift")
const $btnAddItem = document.getElementById("btnAddItem")
const $tbody = document.getElementById("itemTbody")
const $btnSave = document.getElementById("btnSave")
const $btnCancel = document.getElementById("btnCancel")

$btnCancel.addEventListener("click", () => history.back())

// ================== 유틸 ==================
function escapeHtml(s) {
    return String(s ?? "").replace(
        /[&<>"']/g,
        (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[m],
    )
}
function fmtDate(iso) {
    const d = new Date(iso)
    const y = d.getFullYear(),
        m = String(d.getMonth() + 1).padStart(2, "0"),
        day = String(d.getDate()).padStart(2, "0")
    return `${y}-${m}-${day}`
}
function todayStr() {
    const t = new Date()
    const y = t.getFullYear(),
        m = String(t.getMonth() + 1).padStart(2, "0"),
        d = String(t.getDate()).padStart(2, "0")
    return `${y}-${m}-${d}`
}
function toInt(v) {
    const n = Number(String(v).replaceAll(",", ""))
    return Number.isFinite(n) ? n : null
}
function hhmmToMinutes(s) {
    // "HH:mm" 또는 "-HH:mm"
    if (!s) return 0
    const sign = s.startsWith("-") ? -1 : 1
    const [hh, mm] = s.replace("-", "").split(":").map(Number)
    const h = Number.isFinite(hh) ? hh : 0,
        m = Number.isFinite(mm) ? mm : 0
    return sign * (h * 60 + m)
}
// ================== 초기 로드 ==================
;(async function init() {
    document.title = (isEdit ? "계획 수정" : "계획 만들기") + " | TravleSay"
    $btnSave.textContent = isEdit ? "Update" : "Create"

    addProgressIndicator()

    if (isEdit) {
        await loadPlan(planId)
        await loadDays(planId)
        if (state.days.length) await selectDay(state.days[0].id)
        updateProgressIndicator("editing")
    } else {
        $newDayInput.value = todayStr() // 기본값 오늘
        $dayCard.style.display = "none"
        updateProgressIndicator("creating")
    }

    $btnAddDay.onclick = onAddDay
    $btnShift.onclick = onShiftTime
    $btnAddItem.onclick = () => renderNewItemRow()
    $btnSave.onclick = onSavePlan
})()

// ================== Plan ==================
async function loadPlan(id) {
    const data = await httpGet(API_PLAN_DETAIL(id)) // {id,title,isPublic,isCompleted}
    state.plan = { id: data.id, title: data.title, isPublic: data.isPublic, isCompleted: data.isCompleted }
    $title.value = state.plan.title ?? ""
    $isPublic.checked = !!state.plan.isPublic
    $isCompleted.checked = !!state.plan.isCompleted
}

async function onSavePlan() {
    const payload = {
        title: ($title.value ?? "").trim(),
        isPublic: $isPublic.checked,
        isCompleted: $isCompleted.checked,
    }
    if (!payload.title) {
        alert("제목을 입력해주세요.")
        $title.focus()
        return
    }

    if (!isEdit) {
        // 신규 생성
        const created = await httpPost(API_PLAN_CREATE, { title: payload.title, isPublic: payload.isPublic })
        state.plan.id = created.id

        updateProgressIndicator("days")

        // 첫 Day 자동 생성(옵션)
        if (!state.days.length) {
            const tripDate = $newDayInput.value || todayStr()
            await createDay(state.plan.id, tripDate)
        }
        alert("계획이 생성되었습니다!")
        location.href = "/my-plans.html"
    } else {
        // 수정 저장
        await httpPatch(API_PLAN_UPDATE(state.plan.id), payload)
        alert("수정이 저장되었습니다.")
        location.href = "/my-plans.html"
    }
}

// ================== Days ==================
async function loadDays(pid) {
    const data = await httpGet(API_DAYS(pid))
    state.days = data.map((d) => ({ id: d.id ?? d.dayId, tripDate: d.tripDate }))
    renderDayTabs()
}
function renderDayTabs() {
    $tabs.innerHTML = state.days
        .map(
            (d) =>
                `<button class="day-chip ${d.id === state.curDayId ? "active" : ""}" data-id="${d.id}">${fmtDate(d.tripDate)}</button>`,
        )
        .join("")
    $tabs.querySelectorAll(".day-chip").forEach((b) => (b.onclick = () => selectDay(b.dataset.id)))
}
async function selectDay(dayId) {
    state.curDayId = Number(dayId)
    const d = state.days.find((x) => x.id === state.curDayId)
    if (!d) return
    $dayCard.style.display = ""
    $curDayLabel.textContent = fmtDate(d.tripDate)
    renderDayTabs()
    await loadItems(state.curDayId)

    updateProgressIndicator("editing")
}
async function onAddDay() {
    if (!isEdit && !state.plan.id) {
        alert("먼저 계획을 생성하세요.")
        return
    }
    const date = $newDayInput.value
    if (!date) {
        alert("날짜를 선택하세요.")
        return
    }
    await createDay(state.plan.id, date)
    await loadDays(state.plan.id)
    const just = state.days.find((x) => fmtDate(x.tripDate) === date)
    if (just) await selectDay(just.id)
}
async function createDay(planId, tripDate) {
    try {
        await httpPost(API_DAY_CREATE(planId), { tripDate })
    } catch (e) {
        if (e.status === 400) alert(e.message || "이미 같은 날짜가 존재합니다.")
        else throw e
    }
}

// ================== Items ==================
async function loadItems(dayId) {
    const data = await httpGet(API_ITEMS(dayId))
    state.items = data // [{id,title,startTime,amount,merchant,memo,orderNo}]
    renderItems()
}

function fmtAmt(n) {
    if (n == null) return ""
    const v = Number(n)
    return Number.isFinite(v) ? v.toLocaleString() : ""
}

function renderItems() {
    if (!state.items.length) {
        $tbody.innerHTML = `<tr class="empty"><td colspan="6">아직 일정이 없습니다. "아이템 추가" 버튼을 눌러 일정을 추가해보세요! ✈️</td></tr>`
        return
    }
    $tbody.innerHTML = state.items
        .map((it, idx) => {
            const next = state.items[idx + 1]
            const startVal = it.startTime ? formatTimeForInput(it.startTime) : ""
            const nextTimeDisplay = next && next.startTime ? formatTimeForDisplay(next.startTime) : ""

            return `
      <tr data-id="${it.id}">
        <td>
          <div class="item-time">
            <input class="cell-input" type="time" value="${startVal}" data-f="startTime" step="300" />
            ${next ? `<span style="color: #84cc16;">~</span><span class="end-time" title="다음 항목 시작 시각">${nextTimeDisplay}</span>` : ""}
          </div>
        </td>
        <td><input class="cell-input" type="text"  value="${escapeHtml(it.title ?? "")}" data-f="title" placeholder="일정명을 입력하세요" title="${escapeHtml(it.title ?? "")}" /></td>
        <td><input class="cell-input cell-amt" type="text" value="${fmtAmt(it.amount)}" data-f="amount" inputmode="numeric" placeholder="예: 15,000" /></td>
        <td><input class="cell-input" type="text"  value="${escapeHtml(it.merchant ?? "")}" data-f="merchant" placeholder="사용처" title="${escapeHtml(it.merchant ?? "")}" /></td>
        <td><input class="cell-input" type="text"  value="${escapeHtml(it.memo ?? "")}" data-f="memo" placeholder="메모" title="${escapeHtml(it.memo ?? "")}" /></td>
        <td class="row-actions">
          <button class="mini" data-act="up" title="위로 이동">↑</button>
          <button class="mini" data-act="down" title="아래로 이동">↓</button>
          <button class="mini" data-act="addAfter" title="아래에 추가">＋</button>
          <button class="mini" data-act="del" title="삭제">🗑</button>
        </td>
      </tr>
    `
        })
        .join("")
    bindRowEvents()
}

function bindRowEvents() {
    $tbody.querySelectorAll("tr").forEach((tr) => {
        const id = Number(tr.dataset.id)
        tr.querySelectorAll("input[data-f]").forEach((inp) => {
            inp.addEventListener("input", () => {
                if (inp.dataset.f === "title" || inp.dataset.f === "merchant" || inp.dataset.f === "memo") {
                    inp.setAttribute("title", inp.value)
                }
            })

            inp.addEventListener("change", async () => {
                const payload = collectRowPayload(tr)
                await httpPatch(API_ITEM_UPDATE(id), payload)
                await loadItems(state.curDayId) // 다음 항목 end-time 갱신
            })
        })
        tr.querySelectorAll("button[data-act]").forEach((btn) => {
            const act = btn.dataset.act
            btn.onclick = async () => {
                if (act === "del") {
                    if (!confirm("이 아이템을 삭제할까요?")) return
                    await httpDelete(API_ITEM_DELETE(id))
                    await loadItems(state.curDayId)
                } else if (act === "up" || act === "down") {
                    const it = state.items.find((x) => x.id === id)
                    const newOrder = it.orderNo + (act === "up" ? -1 : +1)
                    try {
                        await httpPatch(API_ITEM_REORDER(id), { newOrderNo: newOrder })
                        await loadItems(state.curDayId)
                    } catch (e) {
                        alert("이동할 수 없습니다.")
                        console.error(e)
                    }
                } else if (act === "addAfter") {
                    const it = state.items.find((x) => x.id === id)
                    renderNewItemRow(it.orderNo + 1)
                }
            }
        })
    })
}

function collectRowPayload(tr) {
    const get = (f) => tr.querySelector(`input[data-f="${f}"]`)?.value ?? null
    const amt = toInt(get("amount"))
    return {
        title: get("title")?.trim() || null,
        startTime: get("startTime") || null, // "HH:mm"
        amount: amt,
        merchant: get("merchant")?.trim() || null,
        memo: get("memo")?.trim() || null,
    }
}

function renderNewItemRow(insertPos) {
    if (!state.curDayId) {
        alert("먼저 Day를 선택하세요.")
        return
    }
    const tr = document.createElement("tr")
    tr.innerHTML = `
    <td><div class="item-time"><input class="cell-input" type="time" data-f="startTime" /></div></td>
    <td><input class="cell-input" type="text" data-f="title" placeholder="일정명" /></td>
    <td><input class="cell-input cell-amt" type="text" data-f="amount" placeholder="예: 35000" inputmode="numeric"/></td>
    <td><input class="cell-input" type="text" data-f="merchant" placeholder="사용처"/></td>
    <td><input class="cell-input" type="text" data-f="memo" placeholder="메모"/></td>
    <td class="row-actions">
      <button class="mini" data-act="create">저장</button>
      <button class="mini" data-act="cancel">취소</button>
    </td>
  `
    if ($tbody.querySelector(".empty")) $tbody.innerHTML = ""
    $tbody.appendChild(tr)

    tr.querySelectorAll("input[data-f]").forEach((inp) => {
        if (inp.dataset.f === "title" || inp.dataset.f === "merchant" || inp.dataset.f === "memo") {
            inp.addEventListener("input", () => {
                inp.setAttribute("title", inp.value)
            })
        }
    })

    tr.querySelector('button[data-act="cancel"]').onclick = () => {
        tr.remove()
        if (!$tbody.children.length) renderItems()
    }
    tr.querySelector('button[data-act="create"]').onclick = async () => {
        const payload = collectRowPayload(tr)
        if (!payload.title) {
            alert("일정명을 입력하세요.")
            return
        }
        if (insertPos) payload.orderNo = insertPos
        await httpPost(API_ITEM_CREATE(state.curDayId), payload)
        await loadItems(state.curDayId)
    }
}

// ================== Shift time ==================
async function onShiftTime() {
    if (!state.curDayId) {
        alert("Day를 먼저 선택하세요.")
        return
    }
    const offset = ($shiftOffset.value || "00:00").trim()
    await httpPatch(API_SHIFT(state.curDayId), { offset })
    await loadItems(state.curDayId)
}

// ================== Progress Indicator ==================
function addProgressIndicator() {
    const progressHtml = `
        <div class="pe-progress">
            <div class="progress-step" id="step-plan">
                <span>📝</span>
                <span>계획 정보</span>
            </div>
            <div class="progress-arrow">→</div>
            <div class="progress-step" id="step-days">
                <span>📅</span>
                <span>날짜 추가</span>
            </div>
            <div class="progress-arrow">→</div>
            <div class="progress-step" id="step-schedule">
                <span>⏰</span>
                <span>일정 작성</span>
            </div>
        </div>
    `

    const mainElement = document.querySelector(".pe-wrap")
    mainElement.insertAdjacentHTML("afterbegin", progressHtml)
}

function updateProgressIndicator(stage) {
    const steps = ["step-plan", "step-days", "step-schedule"]
    steps.forEach((stepId) => {
        const element = document.getElementById(stepId)
        element.classList.remove("active", "completed")
    })

    if (stage === "creating") {
        document.getElementById("step-plan").classList.add("active")
    } else if (stage === "days") {
        document.getElementById("step-plan").classList.add("completed")
        document.getElementById("step-days").classList.add("active")
    } else if (stage === "editing") {
        document.getElementById("step-plan").classList.add("completed")
        document.getElementById("step-days").classList.add("completed")
        document.getElementById("step-schedule").classList.add("active")
    }
}

// ================== Enhanced Time Formatting ==================
function formatTimeForInput(timeString) {
    if (!timeString) return ""

    // Handle various time formats and ensure HH:MM format
    const timeStr = String(timeString)

    // If already in HH:MM format, return as is
    if (timeStr.match(/^\d{2}:\d{2}$/)) {
        return timeStr
    }

    // If in HH:MM:SS format, extract HH:MM
    if (timeStr.match(/^\d{2}:\d{2}:\d{2}$/)) {
        return timeStr.substring(0, 5)
    }

    // If it's a time object or other format, try to parse
    try {
        const date = new Date(`2000-01-01T${timeStr}`)
        const hours = String(date.getHours()).padStart(2, "0")
        const minutes = String(date.getMinutes()).padStart(2, "0")
        return `${hours}:${minutes}`
    } catch (e) {
        console.warn("Could not format time:", timeString)
        return ""
    }
}

function formatTimeForDisplay(timeString) {
    if (!timeString) return ""

    try {
        const timeStr = String(timeString)
        let hours, minutes

        // Parse HH:MM format
        if (timeStr.match(/^\d{1,2}:\d{2}$/)) {
            ;[hours, minutes] = timeStr.split(":").map(Number)
        } else if (timeStr.match(/^\d{2}:\d{2}:\d{2}$/)) {
            ;[hours, minutes] = timeStr.substring(0, 5).split(":").map(Number)
        } else {
            const date = new Date(`2000-01-01T${timeStr}`)
            hours = date.getHours()
            minutes = date.getMinutes()
        }

        const period = hours < 12 ? "오전" : "오후"
        const displayHours = hours === 0 ? 12 : hours > 12 ? hours - 12 : hours
        const displayMinutes = String(minutes).padStart(2, "0")

        return `${period} ${displayHours}:${displayMinutes}`
    } catch (e) {
        console.warn("Could not format time for display:", timeString)
        return timeString
    }
}
