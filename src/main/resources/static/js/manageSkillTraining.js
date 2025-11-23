const API_BASE = "http://localhost:8080";

let skillState = { page: 0, size: 10, totalPages: 0 };
let trainingState = { page: 0, size: 10, totalPages: 0 };

async function handleRequest(url, method, body = null) {
    try {
        const options = {
            method: method,
            headers: { "Content-Type": "application/json" }
        };
        if (body) options.body = JSON.stringify(body);
        const response = await fetch(url, options);
        const text = await response.text();
        if (!response.ok) { alert(text); return false; }
        return true;
    } catch (error) {
        console.error(error); return false;
    }
}

// 技能管理

async function loadSkills(page = 0) {
    if (page < 0) page = 0;
    if (skillState.totalPages > 0 && page >= skillState.totalPages) page = skillState.totalPages - 1;

    const res = await fetch(`${API_BASE}/skill/list?page=${page}&size=${skillState.size}`);
    const data = await res.json();

    let list = [];
    if (data.content) {
        list = data.content;
        skillState.page = data.number;
        skillState.totalPages = data.totalPages;
        document.getElementById("skillPageInfo").innerText = `第 ${data.number + 1} / ${data.totalPages || 1} 页`;
    } else {
        list = data; // 兼容 List 返回
    }

    const tbody = document.getElementById("skillTableBody");
    tbody.innerHTML = "";

    if(!list || list.length === 0) {
        tbody.innerHTML = "<tr><td colspan='4' style='text-align:center;color:#999'>暂无数据</td></tr>";
        return;
    }

    list.forEach(item => {
        const safeName = item.skillName.replace(/"/g, '&quot;');
        const safeKind = (item.skillKind || '').replace(/"/g, '&quot;');

        tbody.innerHTML += `
                <tr>
                    <td><b>${item._id}</b></td>
                    <td>${item.skillName}</td>
                    <td><span class="tag tag-skill">${item.skillKind || '通用'}</span></td>
                    <td class="btn-action-group">
                        <button class="btn btn-edit" onclick="editSkill(${item._id}, '${safeName}', '${safeKind}')">编辑</button>
                        <button class="btn btn-danger" onclick="deleteSkill(${item._id})">删除</button>
                    </td>
                </tr>
            `;
    });
}

function changeSkillPage(delta) { loadSkills(skillState.page + delta); }

// 搜索功能
async function searchSkill() {
    const name = document.getElementById("searchSkillName").value;
    if(!name) { loadSkills(0); return; }
    const res = await fetch(`${API_BASE}/skill/search?name=${name}`);
    const list = await res.json();
    // 渲染搜索结果（不分页）
    const tbody = document.getElementById("skillTableBody");
    tbody.innerHTML = "";
    if(!list || list.length === 0) tbody.innerHTML = "<tr><td colspan='4' style='text-align:center;'>无搜索结果</td></tr>";
    list.forEach(item => {
        const safeName = item.skillName.replace(/"/g, '&quot;');
        const safeKind = (item.skillKind || '').replace(/"/g, '&quot;');
        tbody.innerHTML += `<tr><td><b>${item._id}</b></td><td>${item.skillName}</td><td><span class="tag tag-skill">${item.skillKind || '通用'}</span></td><td class="btn-action-group"><button class="btn btn-edit" onclick="editSkill(${item._id}, '${safeName}', '${safeKind}')">编辑</button><button class="btn btn-danger" onclick="deleteSkill(${item._id})">删除</button></td></tr>`;
    });
    document.getElementById("skillPageInfo").innerText = "搜索结果";
}

async function addSkill() {
    const idInput = document.getElementById("skillIdInput").value;
    const name = document.getElementById("skillName").value;
    const kind = document.getElementById("skillKind").value;

    if (!name) return alert("请输入技能名称");
    const payload = { skillName: name, skillKind: kind };
    if(idInput) payload._id = parseInt(idInput);

    const success = await handleRequest(`${API_BASE}/skill/add`, "POST", payload);
    if (success) { resetSkillForm(); loadSkills(0); }
}

async function updateSkill() {
    const id = document.getElementById("skillIdInput").value;
    const name = document.getElementById("skillName").value;
    const kind = document.getElementById("skillKind").value;

    if (!id) return alert("无法获取ID");
    if (!name) return alert("请输入技能名称");

    const success = await handleRequest(`${API_BASE}/skill/update`, "POST", {
        _id: parseInt(id),
        skillName: name,
        skillKind: kind
    });

    if (success) { alert("技能修改成功"); resetSkillForm(); loadSkills(skillState.page); }
}

function editSkill(id, name, kind) {
    document.getElementById("skillIdInput").value = id;
    document.getElementById("skillName").value = name;
    document.getElementById("skillKind").value = kind;

    document.getElementById("skillIdInput").disabled = true;
    document.getElementById("skillEditModeBar").style.display = "block";
    document.getElementById("editSkillIdDisplay").innerText = id;

    document.getElementById("skillBtnGroup").innerHTML = `
            <button class="btn btn-warning" onclick="updateSkill()">保存修改</button>
            <button class="btn btn-cancel" onclick="resetSkillForm()">取消编辑</button>
        `;
}

function resetSkillForm() {
    document.getElementById("skillIdInput").value = "";
    document.getElementById("skillIdInput").disabled = false;
    document.getElementById("skillName").value = "";
    document.getElementById("skillKind").value = "";
    document.getElementById("skillEditModeBar").style.display = "none";
    document.getElementById("skillBtnGroup").innerHTML = `<button class="btn btn-primary" onclick="addSkill()">+ 新增技能</button>`;
}

async function deleteSkill(id) {
    if(!confirm(`确定要删除技能 ID:${id} 吗？`)) return;
    const success = await handleRequest(`${API_BASE}/skill/delete/${id}`, "DELETE");
    if (success) loadSkills(skillState.page);
}


// 培训管理

async function loadTrainings(page = 0) {
    if (page < 0) page = 0;
    if (trainingState.totalPages > 0 && page >= trainingState.totalPages) page = trainingState.totalPages - 1;

    const res = await fetch(`${API_BASE}/training/list?page=${page}&size=${trainingState.size}`);
    const data = await res.json();

    let list = [];
    if (data.content) {
        list = data.content;
        trainingState.page = data.number;
        trainingState.totalPages = data.totalPages;
        document.getElementById("trainingPageInfo").innerText = `第 ${data.number + 1} / ${data.totalPages || 1} 页`;
    } else {
        list = data;
    }

    const tbody = document.getElementById("trainingTableBody");
    tbody.innerHTML = "";

    if(!list || list.length === 0) {
        tbody.innerHTML = "<tr><td colspan='5' style='text-align:center;color:#999'>暂无课程</td></tr>";
        return;
    }

    list.forEach(item => {
        const count = item.members ? item.members.length : 0;
        const memberStr = (item.members || []).join(",");
        const safeName = item.trainName.replace(/"/g, '&quot;');

        tbody.innerHTML += `
                <tr>
                    <td><b>${item._id}</b></td>
                    <td>${item.trainName}</td>
                    <td><span class="tag">${item.skillId}</span></td>
                    <td>${count} 人</td>
                    <td class="btn-action-group">
                        <button class="btn btn-edit" onclick="editTraining(${item._id}, '${safeName}', ${item.skillId}, '${memberStr}')">编辑</button>
                        <button class="btn btn-danger" onclick="deleteTraining(${item._id})">删除</button>
                    </td>
                </tr>
            `;
    });
}

function changeTrainingPage(delta) { loadTrainings(trainingState.page + delta); }

async function searchTraining() {
    const name = document.getElementById("searchTrainName").value;
    if(!name) { loadTrainings(0); return; }
    const res = await fetch(`${API_BASE}/training/search?name=${name}`);
    const list = await res.json();
    const tbody = document.getElementById("trainingTableBody");
    tbody.innerHTML = "";
    if(!list || list.length === 0) tbody.innerHTML = "<tr><td colspan='5' style='text-align:center;'>无搜索结果</td></tr>";
    list.forEach(item => {
        const count = item.members ? item.members.length : 0;
        const memberStr = (item.members || []).join(",");
        const safeName = item.trainName.replace(/"/g, '&quot;');
        tbody.innerHTML += `<tr><td><b>${item._id}</b></td><td>${item.trainName}</td><td><span class="tag">${item.skillId}</span></td><td>${count} 人</td><td class="btn-action-group"><button class="btn btn-edit" onclick="editTraining(${item._id}, '${safeName}', ${item.skillId}, '${memberStr}')">编辑</button><button class="btn btn-danger" onclick="deleteTraining(${item._id})">删除</button></td></tr>`;
    });
    document.getElementById("trainingPageInfo").innerText = "搜索结果";
}

async function addTraining() {
    const idInput = document.getElementById("trainIdInput").value;
    const name = document.getElementById("trainName").value;
    const skillId = document.getElementById("targetSkillId").value;
    const memberStr = document.getElementById("memberIds").value;

    if (!name) return alert("请输入课程名称");
    if (!skillId) return alert("请输入关联的技能ID");

    const members = parseMembers(memberStr);

    const payload = { trainName: name, skillId: parseInt(skillId), members: members };
    if(idInput) payload._id = parseInt(idInput);

    const success = await handleRequest(`${API_BASE}/training/add`, "POST", payload);

    if (success) { resetTrainingForm(); loadTrainings(0); }
}

async function updateTraining() {
    // 修改时从被锁定的输入框获取ID
    const id = document.getElementById("trainIdInput").value;
    const name = document.getElementById("trainName").value;
    const skillId = document.getElementById("targetSkillId").value;
    const memberStr = document.getElementById("memberIds").value;

    if (!id) return alert("ID丢失，请重新操作");
    if (!name) return alert("请输入课程名称");

    const members = parseMembers(memberStr);

    const success = await handleRequest(`${API_BASE}/training/update`, "POST", {
        _id: parseInt(id),
        trainName: name,
        skillId: parseInt(skillId),
        members: members
    });

    if (success) { alert("课程修改成功"); resetTrainingForm(); loadTrainings(trainingState.page); }
}

function editTraining(id, name, skillId, memberStr) {
    // 填充数据
    document.getElementById("trainIdInput").value = id;
    document.getElementById("trainName").value = name;
    document.getElementById("targetSkillId").value = skillId;
    document.getElementById("memberIds").value = memberStr;

    // 锁定ID输入框
    document.getElementById("trainIdInput").disabled = true;
    document.getElementById("trainEditModeBar").style.display = "block";
    document.getElementById("editTrainIdDisplay").innerText = id;

    document.getElementById("trainingBtnGroup").innerHTML = `
            <button class="btn btn-warning" onclick="updateTraining()">保存修改</button>
            <button class="btn btn-cancel" onclick="resetTrainingForm()">取消编辑</button>
        `;
}

function resetTrainingForm() {
    document.getElementById("trainIdInput").value = "";
    document.getElementById("trainIdInput").disabled = false;
    document.getElementById("trainName").value = "";
    document.getElementById("targetSkillId").value = "";
    document.getElementById("memberIds").value = "";

    document.getElementById("trainEditModeBar").style.display = "none";

    document.getElementById("trainingBtnGroup").innerHTML = `<button class="btn btn-primary" onclick="addTraining()">+ 发布课程</button>`;
}

function parseMembers(str) {
    let members = [];
    if (str && str.trim()) {
        members = str.split(/[,，]/).map(s => parseInt(s.trim())).filter(n => !isNaN(n));
    }
    return members;
}

async function deleteTraining(id) {
    if(!confirm(`确定要删除课程 ID:${id} 吗？`)) return;
    const success = await handleRequest(`${API_BASE}/training/delete/${id}`, "DELETE");
    if (success) loadTrainings(trainingState.page);
}

window.onload = function() {
    loadSkills(0);
    loadTrainings(0);
};