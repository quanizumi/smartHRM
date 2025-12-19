package com.murasame.smarthrm.service;
//林 2025.12.19

import com.murasame.smarthrm.dto.EmployeeDTO;
import com.murasame.smarthrm.entity.Employee;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 员工业务层接口（Service）
 * 定义员工实体的CRUD、关联关系处理（项目/任务/培训）及高级查询（模糊查询、分页查询）核心业务逻辑
 */
public interface EmployeeService {

    /**
     * 根据员工ID查询单个员工信息
     * @param id 员工主键ID
     * @return 匹配的Employee实体，无匹配则返回null
     */
    Employee findEmployeeById(Integer id);

    /**
     * 查询所有员工信息（全量列表）
     * @return 所有员工的List集合，无数据则返回空列表
     */
    List<Employee> findAllEmployees();

    /**
     * 查询所有员工信息（全量列表，与findAllEmployees功能一致，适配不同调用场景）
     * @return 所有员工的List集合，无数据则返回空列表
     */
    List<Employee> listAllEmployees();

    /**
     * 新增员工（包含项目/任务/培训关联关系初始化）
     * @param employee 待新增的员工实体（需填充基础信息）
     * @param dto 员工修改DTO（封装关联的项目/任务/培训ID列表）
     */
    void saveEmployee(Employee employee, EmployeeDTO dto);

    /**
     * 更新员工信息（包含项目/任务/培训关联关系同步更新）
     * @param employee 待更新的员工实体（需包含主键ID）
     * @param dto 员工修改DTO（封装更新后的关联ID列表）
     */
    void updateEmployee(Employee employee, EmployeeDTO dto);

    /**
     * 删除员工（包含项目/任务/培训关联关系清理）
     * @param empId 待删除员工的主键ID
     */
    void deleteEmployee(Integer empId);

    /**
     * 根据员工姓名模糊查询员工列表（忽略大小写）
     * @param empName 员工姓名关键词（可为空，为空返回空列表）
     * @return 匹配的员工列表，无匹配则返回空列表
     */
    List<Employee> findEmployeesByName(String empName);

    /**
     * 员工分页查询（按姓名模糊匹配）
     * @param empName 员工姓名关键词（可为空，为空则查询所有员工）
     * @param pageNum 当前页码（前端传入从1开始）
     * @param pageSize 每页展示条数
     * @return 分页结果对象（包含当前页员工数据、总条数、分页参数）
     */
    Page<Employee> listEmployeesWithPage(String empName, int pageNum, int pageSize);
}