package com.murasame.smarthrm.controller;
//林 202512.19

import com.murasame.smarthrm.dao.*;
import com.murasame.smarthrm.dto.EmployeeDTO;
import com.murasame.smarthrm.entity.*;
import com.murasame.smarthrm.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工管理MVC控制器
 * 处理员工列表查询（分页/搜索）、新增/编辑/删除等页面交互请求，
 * 适配员工与部门/项目/任务/培训/技能的关联数据处理，
 * 所有请求路径统一前缀：/employees
 */
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    // 日志组件，记录员工业务操作关键日志（新增/编辑/删除结果、数据查询异常）
    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    // 注入员工业务层，处理员工CRUD及关联数据同步逻辑
    @Autowired
    private EmployeeService employeeService;
    // 注入部门DAO，查询部门列表（用于员工所属部门下拉选择）
    @Autowired
    private DepartmentDao departmentDao;
    // 注入项目DAO，查询项目列表（用于员工关联项目下拉选择）
    @Autowired
    private ProjectDao projectDao;
    // 注入任务DAO，查询任务列表（用于员工负责任务下拉选择）
    @Autowired
    private TaskDao taskDao;
    // 注入培训DAO，查询培训列表（用于员工参与培训下拉选择）
    @Autowired
    private TrainingDao trainingDao;
    // 注入技能DAO，查询技能列表（用于员工技能关联下拉选择）
    @Autowired
    private SkillDao skillDao;

    /**
     * 员工列表页查询（支持姓名模糊搜索、分页）
     * @param model 页面数据模型，传递员工列表、部门列表、分页信息到前端
     * @param empName 员工姓名模糊搜索关键词（非必传）
     * @param pageNum 当前页码（默认1，前端分页控件传入）
     * @param pageSize 每页展示条数（默认10）
     * @return 员工列表页视图名称：employees
     */
    @GetMapping("/")
    public String listEmployees(
            Model model,
            @RequestParam(required = false) String empName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        // 1. 调用业务层查询员工分页数据（支持姓名搜索）
        Page<Employee> empPage = employeeService.listEmployeesWithPage(empName, pageNum, pageSize);
        List<Employee> employees = empPage.getContent();

        // 2. 预处理员工部门信息（补全部门名称、标记部门状态）
        List<Department> departments = departmentDao.findAll();
        for (Employee emp : employees) {
            if (emp.getDepId() == null) {
                emp.setDeptName("未分配");
                emp.setDeptType("unassigned");
            } else {
                boolean found = false;
                for (Department dept : departments) {
                    if (Objects.equals(dept.getId(), emp.getDepId())) {
                        emp.setDeptName(dept.getDepName());
                        emp.setDeptType("normal");
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    emp.setDeptName("部门已删除");
                    emp.setDeptType("deleted");
                }
            }
        }

        // 3. 封装页面展示数据
        model.addAttribute("employees", employees); // 当前页员工列表
        model.addAttribute("departments", departments); // 部门列表（用于筛选）
        model.addAttribute("empName", empName); // 回显搜索关键词
        // 分页参数（Page对象页码从0开始，+1还原为前端习惯的从1开始）
        model.addAttribute("pageNum", empPage.getNumber() + 1);
        model.addAttribute("totalPages", empPage.getTotalPages()); // 总页数
        model.addAttribute("totalElements", empPage.getTotalElements()); // 总条数

        return "employees";
    }

    /**
     * 跳转员工新增页面
     * @param model 页面数据模型，传递下拉选项数据、空DTO到前端
     * @return 员工新增/编辑共用视图名称：employee-mod
     */
    @GetMapping("/add")
    public String toAddEmployee(Model model) {
        // 1. 查询所有下拉选项数据（部门/项目/任务/培训/技能）
        List<Department> departments = departmentDao.findAll();
        List<Project> allProjects = projectDao.findAll();
        List<Task> allTasks = taskDao.findAll();
        List<Training> allTrainings = trainingDao.findAll();
        List<Skill> allSkills = skillDao.findAll();

        // 2. 初始化空DTO（避免前端空指针）
        EmployeeDTO dto = new EmployeeDTO();
        dto.setNewProjectIds(new ArrayList<>());
        dto.setNewManagerTaskIds(new ArrayList<>());
        dto.setNewTrainingIds(new ArrayList<>());
        dto.setSkills("");

        // 3. 封装页面展示数据
        model.addAttribute("formAction", "/employees/add"); // 表单提交路径
        model.addAttribute("pageTitle", "新增员工信息"); // 页面标题
        model.addAttribute("employee", new Employee()); // 空员工对象（防前端报错）
        model.addAttribute("dto", dto); // 表单绑定DTO
        model.addAttribute("departments", departments);
        model.addAttribute("allProjects", allProjects);
        model.addAttribute("allTasks", allTasks);
        model.addAttribute("allTrainings", allTrainings);
        model.addAttribute("allSkills", allSkills);

        return "employee-mod";
    }

    /**
     * 提交新增员工数据
     * @param dto 前端提交的员工表单数据（含基本信息、关联数据）
     * @param br 表单验证结果（校验姓名必填等规则）
     * @param ra 重定向属性，用于跨请求传递提示信息
     * @return 重定向到员工列表页（成功）/新增页（失败）
     */
    @PostMapping("/add")
    public String addEmployee(@Valid EmployeeDTO dto,
                              BindingResult br,
                              RedirectAttributes ra) {
        // 1. 表单验证失败：回显错误信息，返回新增页
        if (br.hasErrors()) {
            ra.addFlashAttribute("errors", br.getFieldErrors());
            return "redirect:/employees/add";
        }

        try {
            // 2. 组装新增员工对象（兼容空值处理）
            Employee employee = new Employee();
            employee.setEmpName(dto.getName()); // 姓名必填

            // 部门ID：为空设为null，非空转换为Integer
            String depIdStr = dto.getDepartment();
            employee.setDepId(StringUtils.hasText(depIdStr) ? Integer.parseInt(depIdStr.trim()) : null);

            // 加入时间：为空默认当前系统时间
            LocalDateTime joinTime = dto.getJoinDate() != null ? dto.getJoinDate() : LocalDateTime.now();
            employee.setJoinDate(joinTime);

            // 初始化关联数据列表（避免空指针）
            employee.setSkillList(new ArrayList<>());
            employee.setProjects(new ArrayList<>());
            employee.setTrainingList(new ArrayList<>());

            // 3. 调用业务层新增员工（含关联数据绑定）
            employeeService.saveEmployee(employee, dto);
            ra.addFlashAttribute("message", "员工「" + dto.getName() + "」新增成功");
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("error", "新增失败：部门ID必须为数字");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "新增失败：" + e.getMessage());
        }

        // 4. 新增成功：重定向到员工列表页
        return "redirect:/employees/";
    }

    /**
     * 跳转员工编辑页面
     * @param id 待编辑员工ID（必传）
     * @param model 页面数据模型，传递员工详情、下拉选项数据到前端
     * @return 员工新增/编辑共用视图名称：employee-mod
     */
    @GetMapping("/mod")
    public String toModEmployee(@RequestParam Integer id, Model model) {
        // 1. 查询待编辑员工详情，不存在则返回列表页
        Employee employee = employeeService.findEmployeeById(id);
        if (employee == null) {
            model.addAttribute("error", "员工不存在");
            return "redirect:/employees/";
        }

        // 2. 查询所有下拉选项数据（部门/项目/任务/培训/技能）
        List<Department> departments = departmentDao.findAll();
        List<Project> allProjects = projectDao.findAll();
        List<Task> allTasks = taskDao.findAll();
        List<Training> allTrainings = trainingDao.findAll();
        List<Skill> allSkills = skillDao.findAll();
        log.info("查询到的所有培训数据: {}", allTrainings);

        // 3. 提取员工现有关联数据（用于回显）
        // 关联项目ID列表（空值防护）
        List<Integer> existingProjectIds = new ArrayList<>();
        if (employee.getProjects() != null && !employee.getProjects().isEmpty()) {
            existingProjectIds = employee.getProjects().stream()
                    .map(projMap -> projMap.get("projId"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        // 负责任务ID列表
        List<Integer> existingTaskIds = taskDao.findByManagerId(id).stream()
                .map(Task::get_id)
                .collect(Collectors.toList());
        // 参与培训ID列表
        List<Integer> existingTrainingIds = trainingDao.findByMemberEmpId(id).stream()
                .map(Training::get_id)
                .collect(Collectors.toList());
        // 技能数据：转换为 "skillId:熟练度" 字符串（空值防护）
        String existingSkillsStr = "";
        if (employee.getSkillList() != null && !employee.getSkillList().isEmpty()) {
            existingSkillsStr = employee.getSkillList().stream()
                    .filter(skillMap -> skillMap.get("skillId") != null && skillMap.get("proficiency") != null)
                    .map(skillMap -> skillMap.get("skillId") + ":" + skillMap.get("proficiency"))
                    .collect(Collectors.joining(","));
        }

        // 4. 封装DTO用于表单回显（兼容部门ID为空的情况）
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getEmpName());
        dto.setDepartment(employee.getDepId() != null ? employee.getDepId().toString() : "");
        dto.setJoinDate(employee.getJoinDate());
        dto.setSkills(existingSkillsStr);
        dto.setNewProjectIds(existingProjectIds);
        dto.setNewManagerTaskIds(existingTaskIds);
        dto.setNewTrainingIds(existingTrainingIds);

        // 5. 封装页面展示数据
        model.addAttribute("formAction", "/employees/mod"); // 表单提交路径
        model.addAttribute("pageTitle", "编辑员工信息"); // 页面标题
        model.addAttribute("employee", employee); // 员工详情
        model.addAttribute("dto", dto); // 表单绑定DTO
        model.addAttribute("departments", departments);
        model.addAttribute("allProjects", allProjects);
        model.addAttribute("allTasks", allTasks);
        model.addAttribute("allTrainings", allTrainings);
        model.addAttribute("allSkills", allSkills);

        return "employee-mod";
    }

    /**
     * 提交编辑员工数据
     * @param dto 前端提交的员工表单数据（含基本信息、关联数据）
     * @param pageNum 原列表页页码（用于编辑后返回对应分页）
     * @param empName 原列表页搜索关键词（用于编辑后回显搜索条件）
     * @param br 表单验证结果（校验姓名必填等规则）
     * @param ra 重定向属性，用于跨请求传递提示信息
     * @return 重定向到员工列表页（成功）/编辑页（失败）
     */
    @PostMapping("/mod")
    public String modEmployee(@Valid EmployeeDTO dto,
                              @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                              @RequestParam(required = false) String empName,
                              BindingResult br,
                              RedirectAttributes ra) {
        // 1. 查询待编辑员工详情，不存在则抛出异常
        Employee oldEmployee = employeeService.findEmployeeById(dto.getId());
        if (oldEmployee == null) {
            throw new RuntimeException("员工不存在");
        }

        // 2. 表单验证失败：回显错误信息，返回编辑页
        if (br.hasErrors()) {
            ra.addFlashAttribute("errors", br.getFieldErrors());
            return "redirect:/employees/mod?id=" + dto.getId();
        }

        try {
            // 3. 组装编辑后员工对象（兼容空值处理）
            Employee employee = new Employee();
            employee.setId(dto.getId());
            employee.setEmpName(dto.getName()); // 姓名必填

            // 部门ID：为空设为null，非空转换为Integer
            if (dto.getDepartment() != null && !dto.getDepartment().trim().isEmpty()) {
                employee.setDepId(Integer.parseInt(dto.getDepartment().trim()));
            } else {
                employee.setDepId(null);
            }

            // 加入时间：为空则沿用旧值
            employee.setJoinDate(dto.getJoinDate() != null ? dto.getJoinDate() : oldEmployee.getJoinDate());

            // 初始化关联数据列表（避免空指针）
            employee.setSkillList(new ArrayList<>());
            employee.setProjects(new ArrayList<>());

            // 4. 调用业务层更新员工（含关联数据绑定）
            employeeService.updateEmployee(employee, dto);
            ra.addFlashAttribute("message", "员工ID:" + dto.getId() + " 更新成功");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            // 编辑失败：返回编辑页，保留分页/搜索参数
            return "redirect:/employees/mod?id=" + dto.getId() + "&pageNum=" + pageNum + (empName != null ? "&empName=" + empName : "");
        }

        // 5. 编辑成功：重定向到列表页，保留分页/搜索参数
        StringBuilder redirectUrl = new StringBuilder("/employees/?pageNum=").append(pageNum);
        if (empName != null && !empName.isEmpty()) {
            redirectUrl.append("&empName=").append(empName);
        }
        return "redirect:" + redirectUrl.toString();
    }

    /**
     * 删除员工（同步清理关联数据）
     * @param id 待删除员工ID（必传）
     * @param pageNum 原列表页页码（用于删除后返回对应分页）
     * @param empName 原列表页搜索关键词（用于删除后回显搜索条件）
     * @param ra 重定向属性，用于跨请求传递删除结果提示
     * @return 重定向到员工列表页
     */
    @PostMapping("/del")
    public String deleteEmployee(
            @RequestParam Integer id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(required = false) String empName,
            RedirectAttributes ra) {
        try {
            // 1. 调用业务层删除员工（含关联数据同步清理）
            employeeService.deleteEmployee(id);
            ra.addFlashAttribute("message", "员工ID:" + id + " 删除成功（含关联数据同步）");
        } catch (RuntimeException e) {
            // 2. 删除失败：传递错误提示
            ra.addFlashAttribute("error", e.getMessage());
        }

        // 3. 重定向到列表页，保留分页/搜索参数
        StringBuilder redirectUrl = new StringBuilder("/employees/?pageNum=" + pageNum);
        if (empName != null && !empName.isEmpty()) {
            redirectUrl.append("&empName=").append(empName);
        }
        return "redirect:" + redirectUrl.toString();
    }
}