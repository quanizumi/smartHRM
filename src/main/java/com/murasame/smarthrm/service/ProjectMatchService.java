package com.murasame.smarthrm.service;

import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;
import com.murasame.smarthrm.entity.Task;

import java.util.List;
import java.util.Map;

/**
 * 项目匹配服务接口
 */
public interface ProjectMatchService {

    /**
     * 根据项目名称匹配项目
     * @param projectName 项目名称关键词
     * @return 匹配的项目列表
     */
    List<Project> matchByProjectName(String projectName);

    /**
     * 根据员工ID查找其参与的项目
     * @param empId 员工ID
     * @return 该员工参与的项目列表
     */
    List<Project> matchByEmployee(Integer empId);

    /**
     * 根据员工技能查找其可以参与的项目
     * @param empId 员工ID
     * @return 该员工可以参与的项目列表
     */
    List<Project> matchAvailableForEmployee(Integer empId);

    /**
     * 创建新项目
     * @param project 项目信息
     * @return 创建的项目
     */
    Project createProject(Project project);

    /**
     * 更新项目信息
     * @param project 项目信息
     * @return 更新后的项目
     */
    Project updateProject(Project project);

    /**
     * 删除项目
     * @param projectId 项目ID
     * @return 是否删除成功
     */
    boolean deleteProject(Integer projectId);

    /**
     * 根据ID获取项目详情
     * @param projectId 项目ID
     * @return 项目详情
     */
    Project getProjectById(Integer projectId);

    /**
     * 获取项目及其任务信息
     * @param projectId 项目ID
     * @return 项目及其任务的映射（key: project, value: tasks列表）
     */
    Map<String, Object> getProjectWithTasks(Integer projectId);

    /**
     * 获取项目列表及其任务信息
     * @param projects 项目列表
     * @return 包含项目及其任务信息的列表
     */
    List<Map<String, Object>> getProjectsWithTasks(List<Project> projects);
}