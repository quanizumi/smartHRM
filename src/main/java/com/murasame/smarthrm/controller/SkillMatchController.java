package com.murasame.smarthrm.controller;

import com.murasame.smarthrm.dao.DepartmentRepo;
import com.murasame.smarthrm.dao.ProjectRepo;
import com.murasame.smarthrm.dao.SkillRepo;
import com.murasame.smarthrm.dto.SkillMatchDTO;
import com.murasame.smarthrm.entity.Department;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;
import com.murasame.smarthrm.entity.Skill;
import com.murasame.smarthrm.service.SkillMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/skillmatch")
@RequiredArgsConstructor
public class SkillMatchController {

	private final SkillMatchService skillMatchService;
	private final SkillRepo skillRepo;
	private final DepartmentRepo depRepo;
	private final ProjectRepo projRepo;

	@GetMapping("/")
	public String skillMatchPage(){
		return "skillmatch";
	}

	/*
	  Post /skillmatch?requiredSkills=1:3,2:5
	  如果想把结果渲染在页面，把 @ResponseBody 去掉，用 Model 传值即可
	 */
	@PostMapping("/")
	@ResponseBody
	public List<Employee> doSkillMatch(@RequestParam String requiredSkills){
		// 简单拆包
		List<SkillMatchDTO> dtoList = SkillMatchDTO.fromString(requiredSkills);
		return skillMatchService.matchBySkills(dtoList);
	}

	// 辅助接口
	/* 仅返回 [{id,skillName}, ...] */
	@GetMapping("/skills")
	@ResponseBody
	public List<Skill> allSkills(){ return skillRepo.findAll(); }

	/* 仅返回 [{id,projName}, ...] */
	@GetMapping("/projects")
	@ResponseBody
	public List<Project> allProjects(){ return projRepo.findAll(); }

	/* 仅返回 [{id,depName}, ...] */
	@GetMapping("/departments")
	@ResponseBody
	public List<Department> allDeps(){ return depRepo.findAll(); }
}
