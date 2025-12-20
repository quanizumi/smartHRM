/* ==========  全局变量  ========== */
let skillOptions = []; // [{id, skillName}, ...]
let projectMap = {}; // {projId: projName}
let depMap = {};     // {depId: depName}

// 翻页
let currentPage = 1;          // 当前页码
let pageSize    = 20;         // 每页条数
let sortAsc     = true;       // true=正序，false=倒序
let fullList    = [];         // 保存后端返回的完整结果

/* ==========  初始化  ========== */
window.onload = function(){
    loadMetaData();   // 拉技能/项目/部门
    addFilterRow();   // 默认留一行
};

/* ==========  加载元数据  ========== */
function loadMetaData(){
    // ① 技能下拉
    fetch('/skillmatch/skills')
        .then(r=>r.json())
        .then(data=> skillOptions = data );
    // ② 项目 map
    fetch('/skillmatch/projects')
        .then(r=>r.json())
        .then(list=> list.forEach(p=> projectMap[p.id] = p.projName) );
    // ③ 部门 map
    fetch('/skillmatch/departments')
        .then(r=>r.json())
        .then(list=> list.forEach(d=> depMap[d.id] = d.depName) );
}

/* ==========  动态增删筛选行（重写）  ========== */
function addFilterRow(){
    const container = document.getElementById('filterContainer');
    const addBtn    = document.getElementById('addBtn');

    // ① 拉技能（无数据时不生成）
    fetch('/skillmatch/skills')
        .then(r=>r.json())
        .then(skills=>{
            if(!skills || skills.length===0) return;   // 没技能不生成

            const currentRows = container.querySelectorAll('.filter-row').length;
            if(currentRows >= skills.length){          // 达到上限
                addBtn.style.display = 'none';         // 隐藏增加按钮
                return;
            }

            const row       = document.createElement('div');
            row.className   = 'filter-row';

            // ② 技能下拉（有效 option）
            const selSkill = document.createElement('select');
            selSkill.name  = 'skill';
            selSkill.innerHTML = skills.map(s=>`<option value="${s._id}">${s.skillName}</option>`).join('');

            // ③ 熟练度滑块（保持你原有结构）
            const slider = document.createElement('input');
            slider.type = 'range'; slider.min = 1; slider.max = 5; slider.value = 3;
            slider.name = 'level'; slider.style.width='100px';
            const levelLabel = document.createElement('span');
            levelLabel.textContent = slider.value;
            slider.oninput = ()=> levelLabel.textContent = slider.value;

            // ④ 删除按钮（保持你原有结构）
            const btnDel = document.createElement('button');
            btnDel.type='button'; btnDel.textContent='删除'; btnDel.className='btn-small';
            btnDel.onclick = ()=> {
                row.remove();
                addBtn.style.display = 'inline';   // 恢复增加按钮
            };

            // ⑤ 拼装一行
            row.append(selSkill, ' 熟练度≥', slider, levelLabel, btnDel);
            container.appendChild(row);

            // ⑥ 刚加满时隐藏按钮
            if(container.querySelectorAll('.filter-row').length >= skills.length){
                addBtn.style.display = 'none';
            }
        });
}

/* ==========  搜索  ========== */
function doSearch(){
    // 拼字符串 1:3,2:5 ...
    const rows = Array.from(document.querySelectorAll('.filter-row'));
    // 拼字符串前，先过滤空值
    const reqArr = rows
        .map(r => {
            const sid = r.querySelector('select[name=skill]').value;
            const lvl = r.querySelector('input[name=level]').value;
            console.log('raw skill=', sid, 'level=', lvl);   // ← 看控制台
            // 只要有一个空，就返回 null，后面统一过滤
            return (sid !== "" && sid !== "undefined" && lvl !== "") ? sid + ':' + lvl : null;
        })
        .filter(Boolean);   // 去掉 null
    if(reqArr.length===0) return alert('请至少添加一条筛选条件');

    // POST 调用
    fetch('/skillmatch/', {
        method: 'POST',
        headers: {'Content-Type':'application/x-www-form-urlencoded'},
        body: 'requiredSkills=' + encodeURIComponent(reqArr.join(','))
    })
        .then(r=>r.json())
        .then(renderTable)
        .catch(err=>alert(err));
}

/* ==========  渲染结果表（带排序+分页）  ========== */
function renderTable(list){
    fullList = list;          // 保存完整结果
    currentPage = 1;          // 每次新搜索后回到第1页
    renderPage();             // 真正渲染逻辑抽出去
}

/* ----------  排序切换  ---------- */
function toggleSort(){
    sortAsc = !sortAsc;
    document.getElementById('sortIcon').textContent = sortAsc ? '↑' : '↓';
    currentPage = 1;   // 重新从第一页显示
    renderPage();
}

/* ----------  真正渲染当前页  ---------- */
function renderPage(){
    // 1. 排序
    fullList.sort((a,b) => sortAsc ? a._id - b._id : b._id - a._id);

    // 2. 分页
    const total   = fullList.length;
    const maxPage = Math.ceil(total / pageSize);
    const start   = (currentPage - 1) * pageSize;
    const end     = start + pageSize;
    const pageData= fullList.slice(start, end);

    // 3. 写表格
    const tbody = document.querySelector('#resultTable tbody');
    tbody.innerHTML = '';
    pageData.forEach(emp => {
        const tr = document.createElement('tr');

        const skillTexts = (emp.skillList || [])
            .map(item => {
                const skill = skillOptions.find(s => s._id == item.skillId);
                return `${skill ? skill.skillName : '未知技能'} - 熟练度:${item.proficiency}`;
            }).join('<br>');

        const projTexts = (emp.projects || [])
            .map(item => projectMap[item.projId] || `项目${item.projId}`)
            .join('<br>');

        tr.innerHTML = `
            <td>${emp._id}</td>
            <td>${emp.empName}</td>
            <td>${depMap[emp.depId] || '未知部门'}</td>
            <td>${skillTexts}</td>
            <td>${projTexts}</td>`;
        tbody.appendChild(tr);
    });

    // 4. 更新分页按钮状态
    document.getElementById('pageInfo').textContent = `第 ${currentPage} 页 / 共 ${maxPage} 页`;
    document.getElementById('btnPrev').disabled = currentPage === 1;
    document.getElementById('btnNext').disabled = currentPage === maxPage;

    document.getElementById('pageInput').value = currentPage;
}

/* ----------  翻页  ---------- */
function changePage(delta){
    const maxPage = Math.ceil(fullList.length / pageSize);
    const newPage = currentPage + delta;
    if (newPage >= 1 && newPage <= maxPage) {
        currentPage = newPage;
        renderPage();
    }
}

/* ----------  首页 / 尾页  ---------- */
function gotoFirst(){
    currentPage = 1;
    renderPage();
}
function gotoLast(){
    currentPage = Math.ceil(fullList.length / pageSize);
    renderPage();
}

/* ----------  输入框跳转  ---------- */
function jumpToPage(){
    const maxPage = Math.ceil(fullList.length / pageSize);
    let pageNum   = parseInt(document.getElementById('pageInput').value, 10);
    if (!isNaN(pageNum) && pageNum >= 1 && pageNum <= maxPage) {
        currentPage = pageNum;
        renderPage();
    } else {
        alert('页码超出范围！');
    }
}