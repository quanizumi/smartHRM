/* ==========  全局变量  ========== */
let skillOptions = []; // [{_id, skillName}, ...]
let empMap = {}; // {empId: empName}
let projectMap = {}; // {projId: projName}

// 翻页
let currentPage = 1;          // 当前页码
let pageSize    = 20;         // 每页条数
let sortAsc     = true;       // true=正序，false=倒序
let fullList    = [];         // 保存后端返回的完整结果

// 项目管理
let selectedProjects = new Set(); // 已选择的项目ID
let currentEditingProject = null; // 当前编辑的项目
let employeesList = []; // 员工列表

/* ==========  初始化  ========== */
window.onload = function(){
    loadMetaData();   // 拉技能/项目/员工
    addFilterRow();   // 默认留一行
};

/* ==========  工具函数  ========== */
// 转换项目状态为文字描述
function getStatusText(status){
    switch(status){
        case 0: return '未归档';
        case 1: return '已归档';
        default: return '未设置';
    }
}

/* ==========  启动时间格式化函数  ========== */
function formatStartDate(startDate) {
    if (!startDate) return '未设置';

    try {
        // 处理LocalDateTime格式
        const date = new Date(startDate);
        if (isNaN(date.getTime())) return '未设置';

        // 格式化日期时间
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch (error) {
        console.error('时间格式化错误:', error);
        return '未设置';
    }
}

// 选择建议项目
function selectSuggestion(projectName, element) {
    const inputContainer = element.closest('.filter-row');
    const inputs = inputContainer.querySelectorAll('input[name="searchValue"]');
    if (inputs.length > 0) {
        const input = inputs[0];
        input.value = projectName;
        if (input.suggestionContainer) {
            input.suggestionContainer.style.display = 'none';
        }
    }
}

/* ==========  加载元数据  ========== */
function loadMetaData(){
    // ① 项目 map
    fetch('/projectmatch/projects')
        .then(r=>r.json())
        .then(list=> list.forEach(p=> {
            const projectId = p._id || p.id || 0;
            projectMap[projectId] = p.projName;
        }) );
    // ② 员工 map
    fetch('/projectmatch/employees')
        .then(r=>r.json())
        .then(list=> list.forEach(e=> empMap[e._id] = e.empName) );
    // ③ 技能数据（用于显示项目所需技能）
    fetch('/projectmatch/skills')
        .then(r=>r.json())
        .then(data=> {
            // 确保数据格式正确，添加安全处理
            skillOptions = data.map(skill => ({
                _id: skill._id || skill.id || 0,
                skillName: skill.skillName || '未知技能'
            }));
        });
}

/* ==========  动态增删筛选行（重写）  ========== */
function addFilterRow(){
    const container = document.getElementById('filterContainer');

    // 检查筛选条件数量（最多10个）
    const existingRows = container.querySelectorAll('.filter-row');
    if (existingRows.length >= 10) {
        alert('最多只能添加10个搜索条件');
        return;
    }

    // 创建新的筛选行
    const row = document.createElement('div');
    row.className = 'filter-row';

    // 搜索类型选择
    const searchType = document.createElement('select');
    searchType.name = 'searchType';
    searchType.className = 'search-type';
    searchType.innerHTML = `
        <option value="projectName">按项目名称搜索</option>
        <option value="empId">按员工ID搜索</option>
    `;

    // 搜索输入框
    const searchInput = document.createElement('input');
    searchInput.type = 'text';
    searchInput.name = 'searchValue';
    searchInput.placeholder = '输入搜索关键词';
    searchInput.className = 'search-input';

    // 删除按钮
    const deleteBtn = document.createElement('button');
    deleteBtn.type = 'button';
    deleteBtn.textContent = '删除';
    deleteBtn.className = 'delete-btn';
    deleteBtn.onclick = function() {
        row.remove();
        // 重新显示增加按钮
        document.getElementById('addBtn').style.display = 'inline-block';
    };

    // 根据搜索类型动态设置输入框样式和提示信息
    searchType.addEventListener('change', function() {
        if (this.value === 'projectName') {
            searchInput.placeholder = '输入项目名称关键词，支持模糊搜索';
            searchInput.className = 'search-input project-input';
            searchInput.type = 'text';
            // 为项目名称搜索添加建议功能
            setupSuggestionBox(searchInput);
        } else {
            searchInput.placeholder = '输入员工ID（如：123）';
            searchInput.className = 'search-input emp-input';
            searchInput.type = 'number';
            // 移除建议功能
            removeSuggestionBox(searchInput);
        }
    });

    // 初始设置
    searchType.dispatchEvent(new Event('change'));

    // 添加到行
    row.appendChild(searchType);
    row.appendChild(searchInput);
    row.appendChild(deleteBtn);

    // 添加到容器
    container.appendChild(row);
}

/* ==========  搜索  ========== */
function doSearch(){
    const rows = Array.from(document.querySelectorAll('.filter-row'));
    if(rows.length===0) return alert('请至少添加一条筛选条件');

    // 收集所有搜索条件
    const searchConditions = rows
        .map(r => {
            const searchType = r.querySelector('select[name=searchType]').value;
            const searchValue = r.querySelector('input[name=searchValue]').value.trim();

            // 验证输入
            if (searchValue === "" || searchValue === "undefined") {
                return null;
            }

            // 员工ID搜索需要验证是数字
            if (searchType === 'empId' && isNaN(parseInt(searchValue))) {
                return null;
            }

            return { type: searchType, value: searchValue };
        })
        .filter(Boolean); // 去掉 null

    if(searchConditions.length===0) return alert('请至少输入一条有效的筛选条件');

    // 逐个执行搜索并合并结果
    const searchPromises = searchConditions.map(condition => {
        // 使用新的API来获取包含任务信息的项目
        let url = '/projectmatch/';
        let options = {
            method: 'POST',
            headers: {'Content-Type':'application/x-www-form-urlencoded'}
        };

        const params = new URLSearchParams();
        params.append('searchType', condition.type);
        params.append('searchValue', condition.value);
        options.body = params.toString();

        return fetch(url, options).then(r => r.json());
    });

    // 并行执行所有搜索
    Promise.all(searchPromises)
        .then(results => {
            // 合并所有结果并去重
            const allProjects = [];
            const projectIds = new Set();

            results.forEach(projectList => {
                projectList.forEach(project => {
                    const projectId = project._id || project.id;
                    if (!projectIds.has(projectId)) {
                        projectIds.add(projectId);
                        allProjects.push(project);
                    }
                });
            });

            // 为每个项目获取任务信息
            return getProjectsWithTasks(allProjects);
        })
        .then(projectsWithTasks => {
            // 显示项目及其任务信息
            renderTableWithTasks(projectsWithTasks);
        })
        .catch(err => alert('搜索出错：' + err));
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

/* ==========  项目名称搜索建议功能  ========== */

// 存储项目数据缓存
let projectCache = null;

/**
 * 设置项目名称搜索建议功能
 * @param {HTMLInputElement} inputElement - 输入框元素
 */
function setupSuggestionBox(inputElement) {
    // 创建建议框容器
    const suggestionContainer = document.createElement('div');
    suggestionContainer.className = 'suggestion-box';
    suggestionContainer.style.display = 'none';
    suggestionContainer.style.position = 'absolute';
    suggestionContainer.style.background = 'white';
    suggestionContainer.style.border = '1px solid #ddd';
    suggestionContainer.style.borderRadius = '4px';
    suggestionContainer.style.maxHeight = '200px';
    suggestionContainer.style.overflowY = 'auto';
    suggestionContainer.style.zIndex = '1000';
    suggestionContainer.style.marginTop = '2px';
    suggestionContainer.style.boxShadow = '0 2px 8px rgba(0,0,0,0.15)';
    suggestionContainer.style.minWidth = '250px';
    suggestionContainer.style.left = '0';
    suggestionContainer.style.top = '100%';

    // 获取父元素并设置相对定位
    const parent = inputElement.parentNode;
    parent.style.position = 'relative';

    // 将建议框添加到父元素
    parent.appendChild(suggestionContainer);

    // 绑定建议功能到输入框
    inputElement.suggestionContainer = suggestionContainer;
    inputElement.parentElement = parent;

    // 绑定事件监听器
    inputElement.addEventListener('input', handleInputChange);
    inputElement.addEventListener('keydown', handleKeyNavigation);
    inputElement.addEventListener('blur', handleBlur);
    inputElement.addEventListener('focus', handleFocus);
}

/**
 * 移除项目名称搜索建议功能
 * @param {HTMLInputElement} inputElement - 输入框元素
 */
function removeSuggestionBox(inputElement) {
    if (inputElement.suggestionContainer) {
        // 移除建议框
        if (inputElement.suggestionContainer.parentNode) {
            inputElement.suggestionContainer.parentNode.removeChild(inputElement.suggestionContainer);
        }

        // 移除事件监听器
        inputElement.removeEventListener('input', handleInputChange);
        inputElement.removeEventListener('keydown', handleKeyNavigation);
        inputElement.removeEventListener('blur', handleBlur);
        inputElement.removeEventListener('focus', handleFocus);

        delete inputElement.suggestionContainer;
        delete inputElement.parentElement;
    }
}

/**
 * 处理输入变化事件
 * @param {Event} event - 输入事件
 */
function handleInputChange(event) {
    const input = event.target;
    const query = input.value.trim();

    if (query.length === 0) {
        hideSuggestions(input);
        return;
    }

    // 延迟执行搜索，避免频繁请求
    clearTimeout(input.suggestionTimeout);
    input.suggestionTimeout = setTimeout(() => {
        searchProjectsForSuggestions(query, input);
    }, 300);
}

/**
 * 搜索项目建议
 * @param {string} query - 搜索查询
 * @param {HTMLInputElement} input - 输入框元素
 */
function searchProjectsForSuggestions(query, input) {
    // 如果没有项目缓存，先加载
    if (!projectCache) {
        fetch('/projectmatch/projects')
            .then(response => response.json())
            .then(projects => {
                projectCache = projects;
                showSuggestions(input, query);
            })
            .catch(error => {
                console.error('加载项目数据失败:', error);
            });
    } else {
        showSuggestions(input, query);
    }
}

/**
 * 显示建议列表
 * @param {HTMLInputElement} input - 输入框元素
 * @param {string} query - 搜索查询
 */
function showSuggestions(input, query) {
    if (!projectCache || !input.suggestionContainer) return;

    const suggestions = projectCache
        .filter(project =>
            project.projName &&
            project.projName.toLowerCase().includes(query.toLowerCase())
        )
        .slice(0, 8); // 最多显示8个建议

    const container = input.suggestionContainer;

    if (suggestions.length === 0) {
        hideSuggestions(input);
        return;
    }

    // 清空现有建议
    container.innerHTML = '';

    // 创建建议项
    suggestions.forEach((project, index) => {
        const suggestionItem = document.createElement('div');
        suggestionItem.className = 'suggestion-item';
        suggestionItem.textContent = project.projName;
        suggestionItem.dataset.projectId = project._id || project.id;

        // 点击选择建议
        suggestionItem.addEventListener('click', () => {
            input.value = project.projName;
            hideSuggestions(input);
            input.focus();
        });

        // 悬停效果
        suggestionItem.addEventListener('mouseenter', () => {
            clearActiveSuggestion(container);
            setActiveSuggestion(container, index);
        });

        container.appendChild(suggestionItem);
    });

    // 确保建议框相对于输入框正确定位
    const inputRect = input.getBoundingClientRect();
    const parentRect = input.parentElement.getBoundingClientRect();

    // 设置建议框宽度与输入框一致
    container.style.width = input.offsetWidth + 'px';
    container.style.left = '0';
    container.style.top = '100%';

    // 显示建议框
    container.style.display = 'block';
}

/**
 * 隐藏建议列表
 * @param {HTMLInputElement} input - 输入框元素
 */
function hideSuggestions(input) {
    if (input.suggestionContainer) {
        input.suggestionContainer.style.display = 'none';
    }
}

/**
 * 处理键盘导航
 * @param {Event} event - 键盘事件
 */
function handleKeyNavigation(event) {
    const container = event.target.suggestionContainer;
    if (!container || container.style.display === 'none') return;

    const suggestions = Array.from(container.querySelectorAll('.suggestion-item'));
    const currentActive = container.querySelector('.suggestion-item.active');
    let currentIndex = suggestions.indexOf(currentActive);

    switch (event.key) {
        case 'ArrowDown':
            event.preventDefault();
            currentIndex = Math.min(currentIndex + 1, suggestions.length - 1);
            clearActiveSuggestion(container);
            setActiveSuggestion(container, currentIndex);
            break;

        case 'ArrowUp':
            event.preventDefault();
            currentIndex = Math.max(currentIndex - 1, 0);
            clearActiveSuggestion(container);
            setActiveSuggestion(container, currentIndex);
            break;

        case 'Enter':
            event.preventDefault();
            if (currentActive) {
                event.target.value = currentActive.textContent;
                hideSuggestions(event.target);
            }
            break;

        case 'Escape':
            hideSuggestions(event.target);
            break;
    }
}

/**
 * 清除活动的建议项
 * @param {HTMLElement} container - 建议容器
 */
function clearActiveSuggestion(container) {
    const active = container.querySelector('.suggestion-item.active');
    if (active) {
        active.classList.remove('active');
    }
}

/**
 * 设置活动的建议项
 * @param {HTMLElement} container - 建议容器
 * @param {number} index - 索引
 */
function setActiveSuggestion(container, index) {
    const suggestions = container.querySelectorAll('.suggestion-item');
    if (suggestions[index]) {
        suggestions[index].classList.add('active');
    }
}

/**
 * 处理失焦事件
 * @param {Event} event - 失焦事件
 */
function handleBlur(event) {
    // 延迟隐藏，以便能够点击建议项
    setTimeout(() => {
        hideSuggestions(event.target);
    }, 200);
}

/**
 * 处理聚焦事件
 * @param {Event} event - 聚焦事件
 */
function handleFocus(event) {
    const query = event.target.value.trim();
    if (query.length > 0) {
        searchProjectsForSuggestions(query, event.target);
    }
}

/* ==========  项目管理功能  ========== */

/**
 * 显示技能预览
 */
function showSkillPreview() {
    const skillsInput = document.getElementById('projectSkills').value.trim();
    const preview = document.getElementById('skillPreview');

    if (!skillsInput) {
        preview.textContent = '';
        return;
    }

    const skillIds = skillsInput.split(',').map(id => id.trim()).filter(id => id);
    const skillNames = skillIds.map(id => {
        const skill = skillOptions.find(s => (s._id || s.id) == parseInt(id));
        return skill ? skill.skillName : `技能${id}`;
    });

    preview.textContent = `预览: ${skillNames.join(', ')}`;
}

/**
 * 显示成员预览
 */
function showMemberPreview() {
    const membersInput = document.getElementById('projectMembers').value.trim();
    const preview = document.getElementById('memberPreview');

    if (!membersInput) {
        preview.textContent = '';
        return;
    }

    const memberIds = membersInput.split(',').map(id => id.trim()).filter(id => id);
    const memberNames = memberIds.map(id => {
        const empName = empMap[id];
        return empName ? empName : `员工${id}`;
    });

    preview.textContent = `预览: ${memberNames.join(', ')}`;
}

/**
 * 刷新数据
 */
function refreshData() {
    loadMetaData();
    showAlert('数据已刷新', 'success');
}

/**
 * 显示创建项目模态框
 */
function showCreateProjectModal() {
    currentEditingProject = null;
    document.getElementById('modalTitle').textContent = '新增项目';
    document.getElementById('projectForm').reset();
    document.getElementById('projectId').value = '';
    document.getElementById('modalAlert').style.display = 'none';

    // 设置默认状态
    document.getElementById('projectStatus').value = '0';

    // 设置当前时间为默认启动时间
    const now = new Date();
    const formatted = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0') + 'T' +
        String(now.getHours()).padStart(2, '0') + ':' +
        String(now.getMinutes()).padStart(2, '0');
    document.getElementById('startDate').value = formatted;

    // 添加输入框事件监听器
    document.getElementById('projectSkills').addEventListener('input', showSkillPreview);
    document.getElementById('projectMembers').addEventListener('input', showMemberPreview);

    document.getElementById('projectModal').style.display = 'block';
}

/**
 * 显示编辑项目模态框
 */
function showEditProjectModal(projectId) {
    currentEditingProject = fullList.find(p => (p._id || p.id) == projectId);
    if (!currentEditingProject) {
        showAlert('项目不存在', 'danger');
        return;
    }

    document.getElementById('modalTitle').textContent = '编辑项目';
    document.getElementById('modalAlert').style.display = 'none';

    // 填充表单数据
    document.getElementById('projectId').value = currentEditingProject._id || currentEditingProject.id;
    document.getElementById('projectName').value = currentEditingProject.projName || '';
    document.getElementById('projectStatus').value = currentEditingProject.projStatus || 0;

    // 处理启动时间
    if (currentEditingProject.startDate) {
        const startDate = new Date(currentEditingProject.startDate);
        const formatted = startDate.getFullYear() + '-' +
            String(startDate.getMonth() + 1).padStart(2, '0') + '-' +
            String(startDate.getDate()).padStart(2, '0') + 'T' +
            String(startDate.getHours()).padStart(2, '0') + ':' +
            String(startDate.getMinutes()).padStart(2, '0');
        document.getElementById('startDate').value = formatted;
    } else {
        document.getElementById('startDate').value = '';
    }

    // 填充技能和成员输入框
    if (currentEditingProject.reqSkill) {
        const skillsStr = currentEditingProject.reqSkill.map(rs => rs.skillId).join(',');
        document.getElementById('projectSkills').value = skillsStr;
    }
    if (currentEditingProject.members) {
        const membersStr = currentEditingProject.members.map(m => m.empId).join(',');
        document.getElementById('projectMembers').value = membersStr;
    }

    // 添加输入框事件监听器
    document.getElementById('projectSkills').addEventListener('input', showSkillPreview);
    document.getElementById('projectMembers').addEventListener('input', showMemberPreview);

    // 显示预览
    showSkillPreview();
    showMemberPreview();

    document.getElementById('projectModal').style.display = 'block';
}

/**
 * 关闭项目模态框
 */
function closeProjectModal() {
    // 移除事件监听器
    const skillsInput = document.getElementById('projectSkills');
    const membersInput = document.getElementById('projectMembers');
    if (skillsInput && membersInput) {
        skillsInput.removeEventListener('input', showSkillPreview);
        membersInput.removeEventListener('input', showMemberPreview);
    }

    // 清空预览
    document.getElementById('skillPreview').textContent = '';
    document.getElementById('memberPreview').textContent = '';

    document.getElementById('projectModal').style.display = 'none';
    currentEditingProject = null;
}



/**
 * 验证ID列表格式
 */
function isValidIdList(input) {
    if (!input || typeof input !== 'string') return false;

    const ids = input.split(',').map(id => id.trim()).filter(id => id);
    if (ids.length === 0) return true; // 空输入是允许的

    return ids.every(id => {
        const num = parseInt(id);
        return !isNaN(num) && num >= 0;
    });
}

/**
 * 保存项目
 */
async function saveProject() {
    // 验证表单
    const projectName = document.getElementById('projectName').value.trim();
    if (!projectName) {
        showAlert('请输入项目名称', 'danger');
        return;
    }

    // 获取输入框值
    const skillsInput = document.getElementById('projectSkills').value.trim();
    const membersInput = document.getElementById('projectMembers').value.trim();

    // 验证技能输入格式
    if (skillsInput && !isValidIdList(skillsInput)) {
        showAlert('技能ID格式错误，请使用数字ID，用英文逗号分隔（如：1,2,3）', 'danger');
        return;
    }

    // 验证成员输入格式
    if (membersInput && !isValidIdList(membersInput)) {
        showAlert('员工ID格式错误，请使用数字ID，用英文逗号分隔（如：101,102,103）', 'danger');
        return;
    }

    // 收集表单数据
    const projectData = {
        projName: projectName,
        projStatus: parseInt(document.getElementById('projectStatus').value),
        startDate: document.getElementById('startDate').value ?
            new Date(document.getElementById('startDate').value) : null,
        reqSkill: [],
        members: []
    };

    // 如果是编辑，保留原有ID
    const projectId = document.getElementById('projectId').value;
    if (projectId) {
        projectData.id = parseInt(projectId);
    }

    // 解析技能输入框
    if (skillsInput) {
        const skillIds = skillsInput.split(',').map(id => id.trim()).filter(id => id && !isNaN(id));
        skillIds.forEach(id => {
            projectData.reqSkill.push({ skillId: parseInt(id) });
        });
    }

    // 解析成员输入框
    if (membersInput) {
        const memberIds = membersInput.split(',').map(id => id.trim()).filter(id => id && !isNaN(id));
        memberIds.forEach(id => {
            projectData.members.push({ empId: parseInt(id) });
        });
    }

    try {
        const url = currentEditingProject ? '/projectmatch/update' : '/projectmatch/create';
        const method = currentEditingProject ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(projectData)
        });

        if (response.ok) {
            const result = await response.json();
            showAlert(currentEditingProject ? '项目更新成功' : '项目创建成功', 'success');
            closeProjectModal();
            refreshData(); // 刷新数据
        } else {
            const error = await response.text();
            showAlert('操作失败: ' + error, 'danger');
        }
    } catch (error) {
        showAlert('网络错误: ' + error.message, 'danger');
    }
}

/**
 * 删除项目
 */
async function deleteProject(projectId) {
    if (!confirm('确定要删除这个项目吗？')) {
        return;
    }

    try {
        const response = await fetch(`/projectmatch/delete/${projectId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showAlert('项目删除成功', 'success');
            refreshData(); // 刷新数据
        } else {
            showAlert('删除失败', 'danger');
        }
    } catch (error) {
        showAlert('网络错误: ' + error.message, 'danger');
    }
}

/**
 * 批量删除项目
 */
async function deleteSelectedProjects() {
    const selectedIds = Array.from(selectedProjects);
    if (selectedIds.length === 0) {
        showAlert('请先选择要删除的项目', 'danger');
        return;
    }

    if (!confirm(`确定要删除选中的 ${selectedIds.length} 个项目吗？`)) {
        return;
    }

    try {
        const response = await fetch('/projectmatch/delete/batch', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(selectedIds)
        });

        if (response.ok) {
            const result = await response.json();
            if (result) {
                showAlert(`成功删除 ${selectedIds.length} 个项目`, 'success');
                selectedProjects.clear();
                updateBatchActions();
                refreshData(); // 刷新数据
            } else {
                showAlert('部分项目删除失败', 'danger');
            }
        } else {
            showAlert('删除失败', 'danger');
        }
    } catch (error) {
        showAlert('网络错误: ' + error.message, 'danger');
    }
}

/**
 * 显示提示信息
 */
function showAlert(message, type = 'info') {
    const alertDiv = document.getElementById('modalAlert');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    alertDiv.style.display = 'block';

    // 3秒后自动隐藏
    setTimeout(() => {
        alertDiv.style.display = 'none';
    }, 3000);
}

/**
 * 切换选择状态
 */
function toggleProjectSelection(projectId, checked) {
    if (checked) {
        selectedProjects.add(projectId);
    } else {
        selectedProjects.delete(projectId);
    }
    updateBatchActions();
}

/**
 * 全选/反选
 */
function toggleSelectAll() {
    const selectAll = document.getElementById('selectAll');
    const headerSelectAll = document.getElementById('headerSelectAll');
    const isChecked = selectAll.checked || headerSelectAll.checked;

    const checkboxes = document.querySelectorAll('.project-checkbox');
    checkboxes.forEach(checkbox => {
        checkbox.checked = isChecked;
        const projectId = checkbox.value;
        if (isChecked) {
            selectedProjects.add(projectId);
        } else {
            selectedProjects.delete(projectId);
        }
    });

    updateBatchActions();
}

/**
 * 更新批量操作栏
 */
function updateBatchActions() {
    const batchActions = document.getElementById('batchActions');
    const headerCheckbox = document.getElementById('headerCheckbox');
    const actionHeader = document.getElementById('actionHeader');
    const selectAll = document.getElementById('selectAll');
    const headerSelectAll = document.getElementById('headerSelectAll');
    const selectedCount = document.getElementById('selectedCount');

    const hasResults = fullList.length > 0;

    if (hasResults) {
        batchActions.style.display = 'flex';
        headerCheckbox.style.display = 'table-cell';
        actionHeader.style.display = 'table-cell';

        // 更新选中计数
        selectedCount.textContent = `已选中 ${selectedProjects.size} 项`;

        // 更新全选状态
        const allChecked = selectedProjects.size > 0 && selectedProjects.size === fullList.length;
        selectAll.checked = allChecked;
        headerSelectAll.checked = allChecked;
    } else {
        batchActions.style.display = 'none';
        headerCheckbox.style.display = 'none';
        actionHeader.style.display = 'none';
    }
}

/**
 * 重写渲染表格函数，添加管理功能
 */
function renderTable(list) {
    fullList = list;          // 保存完整结果
    currentPage = 1;          // 每次新搜索后回到第1页
    renderPage();             // 真正渲染逻辑抽出去
}

/**
 * 重写渲染当前页函数，添加管理功能
 */
function renderPage() {
    // 1. 排序
    fullList.sort((a,b) => {
        const idA = a._id || a.id || 0;
        const idB = b._id || b.id || 0;
        return sortAsc ? idA - idB : idB - idA;
    });

    // 2. 分页
    const total   = fullList.length;
    const maxPage = Math.ceil(total / pageSize);
    const start   = (currentPage - 1) * pageSize;
    const end     = start + pageSize;
    const pageData= fullList.slice(start, end);

    // 3. 写表格
    const tbody = document.querySelector('#resultTable tbody');
    tbody.innerHTML = '';
    pageData.forEach(proj => {
        const tr = document.createElement('tr');

        const skillTexts = (proj.reqSkill || [])
            .map(item => {
                const skill = skillOptions.find(s => s._id == item.skillId);
                return `${skill ? skill.skillName : `技能${item.skillId || '未知'}`}`;
            }).join('<br>');

        const memberTexts = (proj.members || [])
            .map(item => empMap[item.empId] || `员工${item.empId}`)
            .join('<br>');

        const projectId = proj._id || proj.id;
        const isSelected = selectedProjects.has(projectId.toString());

        // 获取任务信息
        const taskCount = proj.taskCount || 0;
        const completedTasks = proj.completedTasks || 0;
        const pendingTasks = proj.pendingTasks || 0;
        const completionRate = taskCount > 0 ? Math.round((completedTasks / taskCount) * 100) : 0;

        const taskStatusText = taskCount > 0
            ? `总计: ${taskCount} | 已完成: ${completedTasks} | 未完成: ${pendingTasks} | 完成率: ${completionRate}%`
            : '暂无任务';

        const taskStatusClass = taskCount === 0 ? 'text-muted' :
            completionRate === 100 ? 'text-success' :
                completionRate >= 50 ? 'text-warning' : 'text-danger';

        tr.innerHTML = `
            <td class="checkbox-col" style="display: table-cell;">
                <input type="checkbox" class="project-checkbox" value="${projectId}" 
                       ${isSelected ? 'checked' : ''} 
                       onchange="toggleProjectSelection('${projectId}', this.checked)">
            </td>
            <td>${projectId || 'N/A'}</td>
            <td>${proj.projName || '未命名项目'}</td>
            <td>${getStatusText(proj.projStatus)}</td>
            <td>${formatStartDate(proj.startDate)}</td>
            <td>${skillTexts}</td>
            <td>${memberTexts}</td>
            <td>
                <div class="task-status-info ${taskStatusClass}">
                    ${taskStatusText}
                </div>
            </td>
            <td>
                <button class="btn-info" onclick="showProjectTasks('${projectId}', '${proj.projName || '未命名项目'}')" 
                        style="padding: 4px 8px; font-size: 12px; background-color: #17a2b8; color: white; border: none; border-radius: 3px; cursor: pointer;">
                    查看任务
                </button>
            </td>
            <td class="action-col" style="display: table-cell; text-align: center;">
                <button class="btn-warning" onclick="showEditProjectModal('${projectId}')" style="margin-right: 5px; padding: 4px 8px; font-size: 12px;">编辑</button>
                <button class="btn-danger" onclick="deleteProject('${projectId}')" style="padding: 4px 8px; font-size: 12px;">删除</button>
            </td>`;
        tbody.appendChild(tr);
    });

    // 4. 更新分页按钮状态
    document.getElementById('pageInfo').textContent = `第 ${currentPage} 页 / 共 ${maxPage} 页`;
    document.getElementById('btnPrev').disabled = currentPage === 1;
    document.getElementById('btnNext').disabled = currentPage === maxPage;

    document.getElementById('pageInput').value = currentPage;

    // 5. 更新批量操作
    updateBatchActions();
}

/**
 * 重写加载元数据函数，添加员工列表加载
 */
function loadMetaData() {
    // ① 项目 map
    fetch('/projectmatch/projects')
        .then(r=>r.json())
        .then(list=> list.forEach(p=> {
            const projectId = p._id || p.id || 0;
            projectMap[projectId] = p.projName;
        }) );

    // ② 员工 map 和列表
    fetch('/projectmatch/employees')
        .then(r=>r.json())
        .then(list=> {
            employeesList = list; // 保存员工列表
            list.forEach(e=> {
                empMap[e._id] = e.empName;
            });
        });

    // ③ 技能数据（用于显示项目所需技能）
    fetch('/projectmatch/skills')
        .then(r=>r.json())
        .then(data=> {
            // 确保数据格式正确，添加安全处理
            skillOptions = data.map(skill => ({
                _id: skill._id || skill.id || 0,
                skillName: skill.skillName || '未知技能'
            }));
        });
}

/* ==========  任务管理功能  ========== */

// 全局变量
let currentProjectTasks = [];
let currentTaskFilter = 'all';
let currentProjectId = null;

/**
 * 显示项目任务
 */
async function showProjectTasks(projectId, projectName) {
    currentProjectId = projectId;
    currentTaskFilter = 'all';

    // 设置模态框标题
    document.getElementById('tasksModalTitle').textContent = `${projectName} - 项目任务`;

    try {
        // 获取项目任务
        const response = await fetch(`/projectmatch/tasks/${projectId}`);
        if (response.ok) {
            currentProjectTasks = await response.json();
            renderTasksList();
            updateTasksSummary();
            document.getElementById('tasksModal').style.display = 'block';
        } else {
            showTasksAlert('获取任务列表失败', 'danger');
        }
    } catch (error) {
        showTasksAlert('网络错误: ' + error.message, 'danger');
    }
}

/**
 * 关闭任务模态框
 */
function closeTasksModal() {
    document.getElementById('tasksModal').style.display = 'none';
    currentProjectTasks = [];
    currentTaskFilter = 'all';
    currentProjectId = null;
}

/**
 * 渲染任务列表
 */
function renderTasksList() {
    const tasksList = document.getElementById('tasksList');
    tasksList.innerHTML = '';

    let filteredTasks = currentProjectTasks;

    // 根据筛选条件过滤任务
    if (currentTaskFilter === 'pending') {
        filteredTasks = currentProjectTasks.filter(task => task.taskStatus === 0);
    } else if (currentTaskFilter === 'completed') {
        filteredTasks = currentProjectTasks.filter(task => task.taskStatus === 1);
    }

    if (filteredTasks.length === 0) {
        tasksList.innerHTML = `
            <div class="no-tasks">
                <p>暂无任务数据</p>
            </div>
        `;
        return;
    }

    filteredTasks.forEach(task => {
        const taskItem = document.createElement('div');
        taskItem.className = 'task-item';

        const statusClass = task.taskStatus === 1 ? 'status-completed' : 'status-pending';
        const statusText = task.taskStatus === 1 ? '已完成' : '未完成';
        const managerName = empMap[task.managerId] || `员工${task.managerId || '未知'}`;

        taskItem.innerHTML = `
            <div class="task-info">
                <div class="task-name">${task.taskName || '未命名任务'}</div>
                <div class="task-details">
                    负责人: ${managerName} | 任务ID: ${task._id || 'N/A'}
                </div>
            </div>
            <div class="task-status">
                <span class="status-badge ${statusClass}">${statusText}</span>
                <div class="task-actions">
                    <button class="btn-edit" onclick="editTask('${task._id}')">编辑</button>
                    <button class="btn-delete" onclick="deleteTask('${task._id}')">删除</button>
                </div>
            </div>
        `;

        tasksList.appendChild(taskItem);
    });
}

/**
 * 更新任务统计信息
 */
function updateTasksSummary() {
    const summary = document.getElementById('tasksSummary');
    const totalTasks = currentProjectTasks.length;
    const completedTasks = currentProjectTasks.filter(task => task.taskStatus === 1).length;
    const pendingTasks = totalTasks - completedTasks;

    summary.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
                <strong>任务统计:</strong> 
                总计 ${totalTasks} 个任务，
                已完成 ${completedTasks} 个，
                未完成 ${pendingTasks} 个
            </div>
            <div>
                完成率: ${totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0}%
            </div>
        </div>
    `;
}

/**
 * 筛选任务
 */
function filterTasks(filterType) {
    currentTaskFilter = filterType;

    // 更新筛选按钮状态
    document.querySelectorAll('.tasks-filters .btn').forEach(btn => {
        btn.classList.remove('active');
    });
    document.getElementById(`filter${filterType.charAt(0).toUpperCase() + filterType.slice(1)}`).classList.add('active');

    renderTasksList();
}

/**
 * 显示任务提示信息
 */
function showTasksAlert(message, type = 'info') {
    const alertDiv = document.getElementById('tasksModalAlert');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    alertDiv.style.display = 'block';

    // 3秒后自动隐藏
    setTimeout(() => {
        alertDiv.style.display = 'none';
    }, 3000);
}

/**
 * 编辑任务
 */
function editTask(taskId) {
    // TODO: 实现任务编辑功能
    showTasksAlert('任务编辑功能待实现', 'info');
}

/**
 * 删除任务
 */
async function deleteTask(taskId) {
    if (!confirm('确定要删除这个任务吗？')) {
        return;
    }

    try {
        const response = await fetch(`/projectmatch/tasks/delete/${taskId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            showTasksAlert('任务删除成功', 'success');
            // 重新加载任务列表
            if (currentProjectId) {
                const reloadResponse = await fetch(`/projectmatch/tasks/${currentProjectId}`);
                if (reloadResponse.ok) {
                    currentProjectTasks = await reloadResponse.json();
                    renderTasksList();
                    updateTasksSummary();
                }
            }
        } else {
            showTasksAlert('删除失败', 'danger');
        }
    } catch (error) {
        showTasksAlert('网络错误: ' + error.message, 'danger');
    }
}

/**
 * 新增任务
 */
function addNewTask() {
    if (!currentProjectId) {
        showTasksAlert('无法获取项目信息', 'danger');
        return;
    }

    // 设置模态框标题
    document.getElementById('createTaskModalTitle').textContent = '新增任务';

    // 清空表单
    document.getElementById('createTaskForm').reset();
    document.getElementById('taskStatus').value = '0'; // 默认未完成
    document.getElementById('createTaskModalAlert').style.display = 'none';

    // 显示模态框
    document.getElementById('createTaskModal').style.display = 'block';
}

/**
 * 关闭任务创建模态框
 */
function closeCreateTaskModal() {
    document.getElementById('createTaskModal').style.display = 'none';
}

/**
 * 保存新任务
 */
async function saveNewTask() {
    // 验证表单
    const taskName = document.getElementById('taskName').value.trim();
    if (!taskName) {
        showCreateTaskAlert('请输入任务名称', 'danger');
        return;
    }

    // 获取任务数据
    const taskData = {
        projId: parseInt(currentProjectId),
        taskName: taskName,
        managerId: document.getElementById('taskManagerId').value ?
            parseInt(document.getElementById('taskManagerId').value) : null,
        taskStatus: parseInt(document.getElementById('taskStatus').value)
    };

    try {
        const response = await fetch('/projectmatch/tasks/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(taskData)
        });

        if (response.ok) {
            showCreateTaskAlert('任务创建成功', 'success');

            // 延迟关闭模态框并刷新任务列表
            setTimeout(() => {
                closeCreateTaskModal();
                // 重新加载项目任务
                if (currentProjectId) {
                    loadProjectTasks(currentProjectId);
                }
            }, 1500);

        } else {
            const error = await response.text();
            showCreateTaskAlert('创建失败: ' + error, 'danger');
        }
    } catch (error) {
        showCreateTaskAlert('网络错误: ' + error.message, 'danger');
    }
}

/**
 * 加载项目任务（内部使用）
 */
async function loadProjectTasks(projectId) {
    try {
        const response = await fetch(`/projectmatch/tasks/${projectId}`);
        if (response.ok) {
            currentProjectTasks = await response.json();
            renderTasksList();
            updateTasksSummary();
        }
    } catch (error) {
        console.error('加载任务失败:', error);
    }
}

/**
 * 显示任务创建提示信息
 */
function showCreateTaskAlert(message, type = 'info') {
    const alertDiv = document.getElementById('createTaskModalAlert');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    alertDiv.style.display = 'block';

    // 3秒后自动隐藏
    setTimeout(() => {
        alertDiv.style.display = 'none';
    }, 3000);
}

/* ==========  项目任务关联功能  ========== */

/**
 * 获取项目列表及其任务信息
 * @param {Array} projects - 项目列表
 * @returns {Promise<Array>} - 包含项目及其任务信息的数组
 */
async function getProjectsWithTasks(projects) {
    try {
        const response = await fetch('/projectmatch/projectsWithTasks', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(projects)
        });

        if (response.ok) {
            return await response.json();
        } else {
            console.error('获取项目任务信息失败');
            return projects.map(project => ({
                project: project,
                tasks: [],
                taskCount: 0,
                completedTasks: 0,
                pendingTasks: 0
            }));
        }
    } catch (error) {
        console.error('网络错误:', error);
        return projects.map(project => ({
            project: project,
            tasks: [],
            taskCount: 0,
            completedTasks: 0,
            pendingTasks: 0
        }));
    }
}

/**
 * 渲染包含任务信息的项目表格
 * @param {Array} projectsWithTasks - 包含项目及其任务信息的数组
 */
function renderTableWithTasks(projectsWithTasks) {
    // 转换为传统格式供现有渲染函数使用
    const simplifiedProjects = projectsWithTasks.map(item => {
        const project = item.project;
        return {
            ...project,
            // 添加任务信息到项目对象
            taskCount: item.taskCount,
            completedTasks: item.completedTasks,
            pendingTasks: item.pendingTasks,
            tasks: item.tasks
        };
    });

    // 使用现有的渲染函数
    renderTable(simplifiedProjects);
}