package com.murasame.smarthrm.controller;

import com.murasame.smarthrm.dao.EmployeeRepo;
import com.murasame.smarthrm.dao.ProjectRepo;
import com.murasame.smarthrm.dao.SkillRepo;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;
import com.murasame.smarthrm.entity.Skill;
import com.murasame.smarthrm.service.ProjectMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/projectmatch")
@RequiredArgsConstructor
public class ProjectMatchController {

    private final ProjectMatchService projectMatchService;
    private final ProjectRepo projectRepo;
    private final EmployeeRepo employeeRepo;
    private final SkillRepo skillRepo;

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
}