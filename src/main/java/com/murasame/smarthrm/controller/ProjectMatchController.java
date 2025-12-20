package com.murasame.smarthrm.controller;

import com.murasame.smarthrm.dao.EmployeeRepo;
import com.murasame.smarthrm.dao.ProjectRepo;
import com.murasame.smarthrm.dao.SkillRepo;
import com.murasame.smarthrm.dao.TaskRepo;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;
import com.murasame.smarthrm.entity.Skill;
import com.murasame.smarthrm.entity.Task;
import com.murasame.smarthrm.service.ProjectMatchService;
import com.murasame.smarthrm.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/projectmatch")
@RequiredArgsConstructor
public class ProjectMatchController {

    private final ProjectMatchService projectMatchService;
    private final TaskService taskService;
    private final ProjectRepo projectRepo;
    private final EmployeeRepo employeeRepo;
    private final SkillRepo skillRepo;
    private final TaskRepo taskRepo;

    @GetMapping("/")
    public String projectMatchPage(){
        return "projectmatch";
    }

    /*
      Post /projectmatch/?searchType=projectName&searchValue=项目关键词
      Post /projectmatch/?searchType=empId&searchValue=123
      如果想把结果渲染在页面，把 @ResponseBody 去掉，用 Model 传值即可
     */
    @PostMapping("/")
    @ResponseBody
    public List<Project> doProjectMatch(
            @RequestParam String searchType,
            @RequestParam String searchValue
    ){
        switch(searchType.toLowerCase()) {
            case "projectname":
                return projectMatchService.matchByProjectName(searchValue);
            case "empid":
                try {
                    Integer empId = Integer.parseInt(searchValue);
                    return projectMatchService.matchByEmployee(empId);
                } catch (NumberFormatException e) {
                    return List.of();
                }
            default:
                return List.of();
        }
    }

    // 辅助接口
    /* 仅返回 [{_id,projName}, ...] */
    @GetMapping("/projects")
    @ResponseBody
    public List<Project> allProjects(){
        return projectRepo.findAll();
    }

    /* 仅返回 [{id,empName}, ...] */
    @GetMapping("/employees")
    @ResponseBody
    public List<Employee> allEmployees(){
        return employeeRepo.findAll();
    }

    /* 仅返回 [{_id,skillName}, ...] */
    @GetMapping("/skills")
    @ResponseBody
    public List<Skill> allSkills(){
        return skillRepo.findAll();
    }

    /* 仅返回部门数据 [{id,depName}, ...] */
    @GetMapping("/departments")
    @ResponseBody
    public List<Object> allDepartments(){
        // 这里需要根据实际的部门数据结构调整
        return List.of();
    }

    // ========== 项目管理接口 ==========

    /**
     * 创建新项目
     */
    @PostMapping("/create")
    @ResponseBody
    public Project createProject(@RequestBody Project project) {
        return projectMatchService.createProject(project);
    }

    /**
     * 更新项目信息
     */
    @PutMapping("/update")
    @ResponseBody
    public Project updateProject(@RequestBody Project project) {
        return projectMatchService.updateProject(project);
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/delete/{projectId}")
    @ResponseBody
    public boolean deleteProject(@PathVariable Integer projectId) {
        return projectMatchService.deleteProject(projectId);
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/detail/{projectId}")
    @ResponseBody
    public Project getProjectDetail(@PathVariable Integer projectId) {
        return projectMatchService.getProjectById(projectId);
    }

    /**
     * 批量删除项目
     */
    @PostMapping("/delete/batch")
    @ResponseBody
    public boolean deleteProjectsBatch(@RequestBody List<Integer> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return false;
        }

        boolean allSuccess = true;
        for (Integer projectId : projectIds) {
            if (!projectMatchService.deleteProject(projectId)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    // ========== 任务管理接口 ==========

    /**
     * 获取项目的所有任务
     */
    @GetMapping("/tasks/{projId}")
    @ResponseBody
    public List<Task> getProjectTasks(@PathVariable Integer projId) {
        return taskService.getTasksByProjectId(projId);
    }

    /**
     * 获取项目的未完成任务
     */
    @GetMapping("/tasks/{projId}/pending")
    @ResponseBody
    public List<Task> getProjectPendingTasks(@PathVariable Integer projId) {
        return taskService.getTasksByProjectIdAndStatus(projId, 0);
    }

    /**
     * 获取项目的已完成任务
     */
    @GetMapping("/tasks/{projId}/completed")
    @ResponseBody
    public List<Task> getProjectCompletedTasks(@PathVariable Integer projId) {
        return taskService.getTasksByProjectIdAndStatus(projId, 1);
    }

    /**
     * 创建新任务
     */
    @PostMapping("/tasks/create")
    @ResponseBody
    public Task createTask(@RequestBody Task task) {
        return taskService.createTask(task);
    }

    /**
     * 更新任务信息
     */
    @PutMapping("/tasks/update")
    @ResponseBody
    public Task updateTask(@RequestBody Task task) {
        return taskService.updateTask(task);
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/tasks/delete/{taskId}")
    @ResponseBody
    public boolean deleteTask(@PathVariable Integer taskId) {
        return taskService.deleteTask(taskId);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/detail/{taskId}")
    @ResponseBody
    public Task getTaskDetail(@PathVariable Integer taskId) {
        return taskService.getTaskById(taskId);
    }

    /**
     * 检查项目是否有任务
     */
    @GetMapping("/tasks/hasTasks/{projId}")
    @ResponseBody
    public boolean hasProjectTasks(@PathVariable Integer projId) {
        return taskService.hasTasks(projId);
    }

    /**
     * 获取负责人管理的任务
     */
    @GetMapping("/tasks/byManager/{managerId}")
    @ResponseBody
    public List<Task> getTasksByManager(@PathVariable Integer managerId) {
        return taskService.getTasksByManagerId(managerId);
    }

    /**
     * 获取项目及其任务信息
     */
    @GetMapping("/projectWithTasks/{projectId}")
    @ResponseBody
    public Map<String, Object> getProjectWithTasks(@PathVariable Integer projectId) {
        return projectMatchService.getProjectWithTasks(projectId);
    }

    /**
     * 获取项目列表及其任务信息
     */
    @PostMapping("/projectsWithTasks")
    @ResponseBody
    public List<Map<String, Object>> getProjectsWithTasks(@RequestBody List<Project> projects) {
        return projectMatchService.getProjectsWithTasks(projects);
    }

    /**
     * 根据项目名称匹配项目（包含任务信息）
     */
    @PostMapping("/searchProjectNameWithTasks")
    @ResponseBody
    public List<Map<String, Object>> searchProjectNameWithTasks(@RequestParam String searchValue) {
        List<Project> projects = projectMatchService.matchByProjectName(searchValue);
        return projectMatchService.getProjectsWithTasks(projects);
    }

    /**
     * 根据员工ID查找其参与的项目（包含任务信息）
     */
    @PostMapping("/searchEmployeeWithTasks")
    @ResponseBody
    public List<Map<String, Object>> searchEmployeeWithTasks(@RequestParam String searchValue) {
        try {
            Integer empId = Integer.parseInt(searchValue);
            List<Project> projects = projectMatchService.matchByEmployee(empId);
            return projectMatchService.getProjectsWithTasks(projects);
        } catch (NumberFormatException e) {
            return List.of();
        }
    }
}