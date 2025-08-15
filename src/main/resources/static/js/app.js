document.addEventListener("DOMContentLoaded", ()=>{
    const listReviews = document.getElementById("listReviews");
    const listPlans   = document.getElementById("listPlans");
    if(!listReviews || !listPlans) return; // 메인 아니면 패스

    const hotReviews = [
        { name:"호랑이선생", views:2880 },
        { name:"고양이",     views:2000 },
        { name:"나떴남래",   views:1500 },
        { name:"곽튜브",     views:1321 },
        { name:"빠니",       views:1221 },
        { name:"윤선생",     views:1111 },
        { name:"임프로",     views: 990 },
    ];
    const hotPlans = [
        { name:"호랑이선생", likes:2880 },
        { name:"고양이",     likes:2000 },
        { name:"나떴남래",   likes:1500 },
        { name:"곽튜브",     likes:1321 },
        { name:"빠니",       likes:1221 },
        { name:"윤선생",     likes:1111 },
        { name:"임프로",     likes: 990 },
    ];

    const row = (initial, title, right, icon)=>{
        const wrap = document.createElement("div"); wrap.className="item";
        const chip = document.createElement("div"); chip.className="chip"; chip.textContent=initial;
        const info = document.createElement("div");
        const t = document.createElement("div"); t.className="title"; t.textContent=title;
        const m = document.createElement("div"); m.className="meta";  m.textContent="";
        info.append(t,m);
        const meta = document.createElement("div"); meta.className="right-meta";
        const i = document.createElement("i"); i.className = icon==="heart" ? "icon-heart" : "icon-eye";
        const n = document.createElement("span"); n.textContent = right.toLocaleString();
        meta.append(i,n);
        wrap.append(chip, info, meta);
        return wrap;
    };

    listReviews.innerHTML = "";
    hotReviews.forEach(({name,views})=> listReviews.appendChild(row("A", name, views, "eye")));

    listPlans.innerHTML = "";
    hotPlans.forEach(({name,likes})=> listPlans.appendChild(row("A", name, likes, "heart")));
});
