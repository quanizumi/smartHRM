package com.murasame.smarthrm.service.impl;

import com.murasame.smarthrm.dao.EmployeeRepo;
import com.murasame.smarthrm.dao.ProjectRepo;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;
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
}