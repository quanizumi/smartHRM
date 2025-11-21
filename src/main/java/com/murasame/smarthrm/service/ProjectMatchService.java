package com.murasame.smarthrm.service;

import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.entity.Project;

import java.util.List;

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
}