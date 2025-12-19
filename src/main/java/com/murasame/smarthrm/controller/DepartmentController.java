package com.murasame.smarthrm.controller;

import com.murasame.smarthrm.dto.DepartmentDTO;
import com.murasame.smarthrm.entity.Department;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.service.DepartmentService;
import com.murasame.smarthrm.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 部门管理MVC控制器
 * 处理部门列表查询、新增/编辑/删除等页面交互请求，适配前端分页、搜索、表单回显等场景，
 * 所有请求路径统一前缀：/departments
 */
@Controller
@RequestMapping("/departments")
public class DepartmentController {

    // 注入部门业务层，处理部门核心业务逻辑
    @Autowired
    private DepartmentService departmentService;

    // 注入员工业务层，处理员工关联查询（如部门员工回显、全量员工列表）
    @Autowired
    private EmployeeService employeeService;

    /**
     * 部门列表页查询（支持模糊搜索、分页）
     * @param searchKey 部门名称模糊搜索关键词（非必传）
     * @param pageNum 当前页码（默认1，前端分页控件传入）
     * @param pageSize 每页展示条数（默认10）
     * @param model 页面数据模型，用于传递部门列表、分页信息到前端
     * @return 部门列表页视图名称：departments
     */
    @GetMapping("/")
    public String listDepartments(
            @RequestParam(required = false) String searchKey,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {
        // 1. 调用业务层查询部门分页数据（支持搜索）
        Page<DepartmentDTO> deptPage = departmentService.listDepartments(searchKey, pageNum, pageSize);

        // 2. 封装页面展示数据
        model.addAttribute("departments", deptPage.getContent()); // 当前页部门列表
        model.addAttribute("searchKey", searchKey); // 回显搜索关键词
        // 分页参数（MongoDB分页页码从0开始，+1还原为前端习惯的从1开始）
        model.addAttribute("pageNum", deptPage.getNumber() + 1);
        model.addAttribute("totalPages", deptPage.getTotalPages()); // 总页数
        model.addAttribute("totalElements", deptPage.getTotalElements()); // 总条数

        // 3. 返回部门列表页视图
        return "departments";
    }

    /**
     * 跳转部门新增/编辑页面
     * @param id 部门ID（编辑时必传，新增时为null）
     * @param pageNum 原列表页页码（用于保存后返回对应分页，默认1）
     * @param searchKey 原列表页搜索关键词（用于保存后回显搜索条件）
     * @param model 页面数据模型，传递部门信息、员工列表到前端
     * @return 部门新增/编辑页视图名称：department-mod
     */
    @GetMapping({"/add", "/mod"})
    public String toModDepartment(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false) String searchKey,
            Model model) {
        // 1. 封装部门对象（新增为空对象，编辑时查询部门详情）
        Department dept = id != null ? departmentService.getDepartmentById(id) : new Department();

        // 2. 查询全量员工列表（用于前端员工选择组件）
        List<Employee> allEmps = employeeService.listAllEmployees();

        // 3. 查询当前部门已关联员工（编辑时回显选中状态）
        List<Employee> selectedEmps = departmentService.getEmployeesByDeptId(id);

        // 4. 封装页面展示数据
        model.addAttribute("dept", dept); // 部门基础信息
        model.addAttribute("allEmps", allEmps); // 全量员工列表
        model.addAttribute("selectedEmps", selectedEmps); // 已选中员工列表
        model.addAttribute("pageNum", pageNum); // 原列表页页码
        model.addAttribute("searchKey", searchKey); // 原列表页搜索关键词

        // 5. 返回部门新增/编辑页视图
        return "department-mod";
    }

    /**
     * 保存部门（支持新增/编辑）
     * @param dept 前端提交的部门表单数据（含ID则为编辑，无ID则为新增）
     * @param pageNum 原列表页页码（用于保存后返回对应分页）
     * @param searchKey 原列表页搜索关键词（用于保存后回显搜索条件）
     * @param ra 重定向属性，用于跨请求传递提示信息、表单回显数据
     * @return 重定向到部门列表页/编辑页（成功→列表页，失败→编辑页）
     */
    @PostMapping("/save")
    public String saveDepartment(
            Department dept,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(required = false) String searchKey,
            RedirectAttributes ra) {
        try {
            // 1. 调用业务层保存部门（新增/编辑分支）
            if (dept.getId() == null) {
                departmentService.saveDepartment(dept);
                ra.addFlashAttribute("success", "新增部门成功！");
            } else {
                departmentService.updateDepartment(dept);
                ra.addFlashAttribute("success", "更新部门成功！");
            }
        } catch (Exception e) {
            // 2. 保存失败：回显错误信息和表单数据
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("dept", dept); // 回显表单数据
            ra.addFlashAttribute("allEmps", employeeService.listAllEmployees()); // 回显员工列表
            if (dept.getId() != null) {
                ra.addFlashAttribute("selectedEmps", departmentService.getEmployeesByDeptId(dept.getId()));
            }

            // 3. 重定向回编辑页，携带分页/搜索参数
            String redirectUrl = "/departments/mod?id=" + dept.getId() + "&pageNum=" + pageNum;
            if (searchKey != null && !searchKey.isEmpty()) {
                redirectUrl += "&searchKey=" + searchKey;
            }
            return "redirect:" + redirectUrl;
        }

        // 4. 保存成功：重定向回列表页，携带分页/搜索参数
        StringBuilder redirectUrl = new StringBuilder("/departments/?pageNum=" + pageNum);
        if (searchKey != null && !searchKey.isEmpty()) {
            redirectUrl.append("&searchKey=").append(searchKey);
        }
        return "redirect:" + redirectUrl.toString();
    }

    /**
     * 删除部门
     * @param id 待删除部门ID（必传）
     * @param pageNum 原列表页页码（用于删除后返回对应分页）
     * @param searchKey 原列表页搜索关键词（用于删除后回显搜索条件）
     * @param redirectAttributes 重定向属性，传递删除结果提示
     * @return 重定向到部门列表页
     */
    @PostMapping("/delete")
    public String deleteDepartment(
            @RequestParam Integer id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(required = false) String searchKey,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. 调用业务层删除部门（同步清理员工关联）
            departmentService.deleteDepartment(id);
            redirectAttributes.addFlashAttribute("success", "部门删除成功！");
        } catch (RuntimeException e) {
            // 2. 删除失败：传递错误提示
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        // 3. 重定向回列表页，携带分页/搜索参数
        StringBuilder redirectUrl = new StringBuilder("/departments/?pageNum=" + pageNum);
        if (searchKey != null && !searchKey.isEmpty()) {
            redirectUrl.append("&searchKey=").append(searchKey);
        }
        return "redirect:" + redirectUrl.toString();
    }

    /**
     * 工具方法：转换前端员工ID字符串为部门实体所需格式
     * @param empListStr 前端传递的员工ID拼接字符串（格式："1,2,3"）
     * @return 部门实体的empList格式：[{"empId":1}, {"empId":2}, ...]，空值/无效值返回空列表
     */
    private List<Map<String, Integer>> convertEmpListStr(String empListStr) {
        List<Map<String, Integer>> empList = new ArrayList<>();
        if (empListStr == null || empListStr.trim().isEmpty()) {
            return empList;
        }

        // 分割字符串并转换为Map格式
        String[] empIdArr = empListStr.split(",");
        for (String empIdStr : empIdArr) {
            try {
                Integer empId = Integer.parseInt(empIdStr.trim());
                empList.add(Map.of("empId", empId));
            } catch (NumberFormatException e) {
                // 忽略无效的员工ID（如非数字）
                continue;
            }
        }
        return empList;
    }
}