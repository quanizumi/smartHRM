/* ==========  全局变量  ========== */
let skillOptions = []; // [{_id, skillName}, ...]
let empMap = {}; // {empId: empName}
let projectMap = {}; // {projId: projName}

// 翻页
let currentPage = 1;          // 当前页码
let pageSize    = 20;         // 每页条数
let sortAsc     = true;       // true=正序，false=倒序
let fullList    = [];         // 保存后端返回的完整结果

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
        const params = new URLSearchParams();
        params.append('searchType', condition.type);
        params.append('searchValue', condition.value);
        
        return fetch('/projectmatch/', {
            method: 'POST',
            headers: {'Content-Type':'application/x-www-form-urlencoded'},
            body: params.toString()
        }).then(r => r.json());
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
            
            renderTable(allProjects);
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

/* ----------  真正渲染当前页  ---------- */
function renderPage(){
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

        tr.innerHTML = `
            <td>${proj._id || proj.id || 'N/A'}</td>
            <td>${proj.projName || '未命名项目'}</td>
            <td>${getStatusText(proj.projStatus)}</td>
            <td>${formatStartDate(proj.startDate)}</td>
            <td>${skillTexts}</td>
            <td>${memberTexts}</td>`;
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