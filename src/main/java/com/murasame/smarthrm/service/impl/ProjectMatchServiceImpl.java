package com.murasame.smarthrm.service.impl;

import com.murasame.smarthrm.dao.EmployeeRepo;
import com.murasame.smarthrm.dao.ProjectRepo;
import com.murasame.smarthrm.dao.TaskRepo;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;
import com.murasame.smarthrm.entity.Task;
import com.murasame.smarthrm.service.ProjectMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目匹配服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProjectMatchServiceImpl implements ProjectMatchService {

    private final ProjectRepo projectRepo;
    private final EmployeeRepo employeeRepo;
    private final TaskRepo taskRepo;

    @Override
    public List<Project> matchByProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有项目
        List<Project> allProjects = projectRepo.findAll();

        // 通过项目名称进行模糊匹配
        String searchTerm = projectName.trim().toLowerCase();
        return allProjects.stream()
                .filter(project -> {
                    String projName = project.getProjName();
                    return projName != null && projName.toLowerCase().contains(searchTerm);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> matchByEmployee(Integer empId) {
        if (empId == null) {
            return new ArrayList<>();
        }

        // 获取指定员工
        Employee employee = employeeRepo.findById(empId).orElse(null);
        if (employee == null) {
            return new ArrayList<>();
        }

        // 获取所有项目
        List<Project> allProjects = projectRepo.findAll();

        // 筛选员工参与的项目
        return allProjects.stream()
                .filter(project -> isEmployeeInProject(project, empId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> matchAvailableForEmployee(Integer empId) {
        if (empId == null) {
            return new ArrayList<>();
        }

        // 获取指定员工
        Employee employee = employeeRepo.findById(empId).orElse(null);
        if (employee == null) {
            return new ArrayList<>();
        }

        // 获取员工技能
        List<Integer> employeeSkills = getEmployeeSkills(employee);

        if (employeeSkills.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有项目
        List<Project> allProjects = projectRepo.findAll();

        // 筛选员工可以参与的项目（不在当前项目中且技能匹配）
        return allProjects.stream()
                .filter(project -> !isEmployeeInProject(project, empId))
                .filter(project -> isEmployeeSkillsMatchProject(project, employeeSkills))
                .collect(Collectors.toList());
    }

    /**
     * 解析技能需求字符串 "1:3,2:5" -> {1:3, 2:5}
     */
    private Map<Integer, Integer> parseSkillRequirements(String skillStr) {
        Map<Integer, Integer> skillMap = new HashMap<>();

        if (skillStr == null || skillStr.trim().isEmpty()) {
            return skillMap;
        }

        String[] skillPairs = skillStr.split(",");
        for (String pair : skillPairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            String[] parts = pair.split(":");
            if (parts.length == 2) {
                try {
                    Integer skillId = Integer.parseInt(parts[0].trim());
                    Integer minLevel = Integer.parseInt(parts[1].trim());
                    skillMap.put(skillId, minLevel);
                } catch (NumberFormatException e) {
                    // 忽略格式错误的条目
                    continue;
                }
            }
        }

        return skillMap;
    }

    /**
     * 检查项目是否匹配所需技能
     */
    private boolean isProjectMatchSkills(Project project, Map<Integer, Integer> requiredSkills) {
        if (project.getReqSkill() == null || project.getReqSkill().isEmpty()) {
            return false;
        }

        // 检查项目所需技能是否包含所有必需技能
        Set<Integer> projectSkills = project.getReqSkill().stream()
                .map(reqSkill -> reqSkill.getSkillId())
                .collect(Collectors.toSet());

        return projectSkills.containsAll(requiredSkills.keySet());
    }

    /**
     * 检查员工是否在项目中
     */
    private boolean isEmployeeInProject(Project project, Integer empId) {
        if (project.getMembers() == null || project.getMembers().isEmpty()) {
            return false;
        }

        return project.getMembers().stream()
                .anyMatch(member -> empId.equals(member.getEmpId()));
    }

    /**
     * 检查员工技能是否匹配项目需求
     */
    private boolean isEmployeeSkillsMatchProject(Project project, List<Integer> employeeSkills) {
        if (project.getReqSkill() == null || project.getReqSkill().isEmpty()) {
            return false;
        }

        Set<Integer> projectRequiredSkills = project.getReqSkill().stream()
                .map(reqSkill -> reqSkill.getSkillId())
                .collect(Collectors.toSet());

        return employeeSkills.stream()
                .anyMatch(skillId -> projectRequiredSkills.contains(skillId));
    }

    /**
     * 获取员工技能列表
     */
    private List<Integer> getEmployeeSkills(Employee employee) {
        if (employee.getSkillList() == null || employee.getSkillList().isEmpty()) {
            return new ArrayList<>();
        }

        return employee.getSkillList().stream()
                .map(skillMap -> {
                    // 处理 Map<String, Integer> 结构的技能列表
                    if (skillMap.containsKey("skillId")) {
                        return skillMap.get("skillId");
                    } else if (skillMap.containsKey("id")) {
                        return skillMap.get("id");
                    } else {
                        // 如果没有标准键名，取第一个值
                        return skillMap.values().iterator().next();
                    }
                })
                .filter(Objects::nonNull)
                .map(obj -> {
                    try {
                        return Integer.parseInt(obj.toString());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Project createProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("项目信息不能为空");
        }

        // 检查项目名称是否重复
        if (project.getProjName() != null && projectRepo.existsByProjName(project.getProjName())) {
            throw new IllegalArgumentException("项目名称已存在");
        }

        // 生成新的ID（如果需要）
        if (project.getId() == null) {
            // 获取当前最大ID + 1
            List<Project> allProjects = projectRepo.findAll();
            Integer maxId = allProjects.stream()
                    .map(Project::getId)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            project.setId(maxId + 1);
        }

        return projectRepo.save(project);
    }

    @Override
    public Project updateProject(Project project) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("项目ID和项目信息不能为空");
        }

        // 检查项目是否存在
        Project existingProject = projectRepo.findById(project.getId()).orElse(null);
        if (existingProject == null) {
            throw new IllegalArgumentException("项目不存在");
        }

        // 检查项目名称是否重复（排除当前项目）
        if (project.getProjName() != null && !project.getProjName().equals(existingProject.getProjName())) {
            if (projectRepo.existsByProjName(project.getProjName())) {
                throw new IllegalArgumentException("项目名称已存在");
            }
        }

        return projectRepo.save(project);
    }

    @Override
    public boolean deleteProject(Integer projectId) {
        if (projectId == null) {
            return false;
        }

        // 检查项目是否存在
        if (!projectRepo.existsById(projectId)) {
            return false;
        }

        try {
            projectRepo.deleteById(projectId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Project getProjectById(Integer projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepo.findById(projectId).orElse(null);
    }

    @Override
    public Map<String, Object> getProjectWithTasks(Integer projectId) {
        if (projectId == null) {
            return new HashMap<>();
        }

        // 获取项目信息
        Project project = projectRepo.findById(projectId).orElse(null);
        if (project == null) {
            return new HashMap<>();
        }

        // 获取项目任务
        List<Task> tasks = taskRepo.findByProjId(projectId);

        Map<String, Object> result = new HashMap<>();
        result.put("project", project);
        result.put("tasks", tasks);
        result.put("taskCount", tasks.size());
        result.put("completedTasks", tasks.stream().filter(task -> task.getTaskStatus() == 1).count());
        result.put("pendingTasks", tasks.stream().filter(task -> task.getTaskStatus() == 0).count());

        return result;
    }

    @Override
    public List<Map<String, Object>> getProjectsWithTasks(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return new ArrayList<>();
        }

        return projects.stream().map(project -> {
            Map<String, Object> projectWithTasks = new HashMap<>();
            projectWithTasks.put("project", project);

            // 获取项目任务
            List<Task> tasks = taskRepo.findByProjId(project.getId());
            projectWithTasks.put("tasks", tasks);
            projectWithTasks.put("taskCount", tasks.size());
            projectWithTasks.put("completedTasks", tasks.stream().filter(task -> task.getTaskStatus() == 1).count());
            projectWithTasks.put("pendingTasks", tasks.stream().filter(task -> task.getTaskStatus() == 0).count());

            return projectWithTasks;
        }).collect(Collectors.toList());
    }
}