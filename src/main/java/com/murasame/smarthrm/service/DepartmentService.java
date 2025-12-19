package com.murasame.smarthrm.service;
//林2025.12.19
import com.murasame.smarthrm.dto.DepartmentDTO;
import com.murasame.smarthrm.entity.Department;
import com.murasame.smarthrm.entity.Employee;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 部门业务层接口（Service）
 * 定义部门实体的CRUD、员工关联处理（同步员工depId）、负责人校验及分页查询等核心业务逻辑
 */
public interface DepartmentService {

    /**
     * 根据部门ID查询单个部门信息
     * @param id 部门主键ID
     * @return 匹配的Department实体，无匹配则返回null
     */
    Department getDepartmentById(Integer id);

    /**
     * 根据部门ID查询该部门下的所有员工
     * @param id 部门主键ID
     * @return 该部门下的员工列表，无员工则返回空列表
     */
    List<Employee> getEmployeesByDeptId(Integer id);

    /**
     * 新增部门
     * @param dept 待新增的部门实体（需填充部门名称、负责人ID等基础信息）
     */
    void saveDepartment(Department dept);

    /**
     * 更新部门信息
     * 核心逻辑：
     * 1. 增删部门下员工；
     * 2. 校验部门负责人有效性；
     * 3. 同步更新员工的depId字段（关联员工所属部门）
     * @param newDept 待更新的部门实体（必须包含主键ID）
     */
    void updateDepartment(Department newDept);

    /**
     * 删除部门
     * 核心逻辑：删除部门的同时，同步将该部门下所有员工的depId字段置空
     * @param deptId 待删除部门的主键ID
     */
    void deleteDepartment(Integer deptId);

    /**
     * 部门分页查询（支持名称模糊匹配）
     * @param searchKey 部门名称关键词（可为空，为空则查询所有部门）
     * @param pageNum 当前页码（前端传入从1开始）
     * @param pageSize 每页展示条数
     * @return 分页结果对象（封装DepartmentDTO，包含部门、负责人、员工简要信息）
     */
    Page<DepartmentDTO> listDepartments(String searchKey, int pageNum, int pageSize);
}