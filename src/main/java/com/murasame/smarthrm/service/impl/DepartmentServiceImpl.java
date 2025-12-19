package com.murasame.smarthrm.service.impl;
//林2025.12.19

import com.murasame.smarthrm.dao.*;
import com.murasame.smarthrm.dto.DepartmentDTO;
import com.murasame.smarthrm.entity.Department;
import com.murasame.smarthrm.entity.Employee;
import com.murasame.smarthrm.service.DepartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门业务层实现类
 * 实现DepartmentService接口，处理部门CRUD、员工跨部门迁移同步（清理原部门关联）、负责人合法性校验等核心业务，
 * 通过@Transactional保证关联操作的事务一致性，支持部门ID自增、分页查询、DTO格式转换等扩展功能
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {
    // 日志组件，记录部门业务操作的关键日志（如ID生成、员工迁移、数据校验）
    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    // 注入部门数据访问层，处理部门实体的数据库CRUD操作
    @Autowired
    private DepartmentDao departmentDao;
    // 注入员工数据访问层，处理员工关联查询、部门ID批量更新等操作
    @Autowired
    private EmployeeDao employeeDao;
    // 注入MongoTemplate，辅助ID生成和数据库原生查询（备用）
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据部门ID查询单个部门信息
     * @param id 部门主键ID
     * @return 匹配的Department实体，ID为null/不存在时返回null
     */
    @Override
    public Department getDepartmentById(Integer id) {
        if (id == null) {
            log.warn("查询部门失败：部门ID为null");
            return null;
        }
        return departmentDao.findById(id);
    }

    /**
     * 根据部门ID查询该部门下的所有员工
     * @param id 部门主键ID
     * @return 该部门下的员工列表，ID为null/无关联员工时返回空列表
     */
    @Override
    public List<Employee> getEmployeesByDeptId(Integer id) {
        if (id == null) {
            log.warn("查询部门员工失败：部门ID为null");
            return new ArrayList<>();
        }

        // 1. 查询部门基础信息
        Department department = departmentDao.findById(id);
        if (department == null || department.getEmpList() == null || department.getEmpList().isEmpty()) {
            log.info("部门ID: {} 无关联员工", id);
            return new ArrayList<>();
        }

        // 2. 提取部门关联的员工ID列表（过滤无效ID）
        List<Integer> empIds = department.getEmpList().stream()
                .map(empMap -> empMap.get("empId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. 批量查询员工信息并返回
        return employeeDao.findByIds(empIds);
    }

    /**
     * 新增部门（含ID自增、员工关联同步、负责人校验）
     * 核心逻辑：
     * 1. 转换前端传递的empIds为部门empList格式；
     * 2. 生成自增部门ID（无ID时）；
     * 3. 校验关联员工存在性，同步迁移员工至新部门（清理原部门关联）；
     * 4. 校验负责人合法性，最终保存部门
     * @param dept 待新增的部门实体（含名称、关联员工ID、负责人ID等）
     */
    @Override
    @Transactional
    public void saveDepartment(Department dept) {
        // 1. 转换empIds为empList格式（适配数据库存储结构）
        if (dept.getEmpIds() != null && !dept.getEmpIds().isEmpty()) {
            List<Map<String, Integer>> empList = dept.getEmpIds().stream()
                    .map(empId -> {
                        Map<String, Integer> map = new HashMap<>();
                        map.put("empId", empId);
                        return map;
                    })
                    .collect(Collectors.toList());
            dept.setEmpList(empList);
        } else {
            dept.setEmpList(new ArrayList<>());
        }

        // 2. 生成自增部门ID（无ID时）
        if (dept.getId() == null) {
            Integer newDeptId = generateDeptId();
            dept.setId(newDeptId);
            log.info("新增部门：生成自增ID = {}", newDeptId);
        }

        // 3. 提取并校验关联员工，同步迁移员工至新部门
        List<Integer> newEmpIds = getEmpIdsFromList(dept.getEmpList());
        if (newEmpIds.isEmpty()) {
            log.warn("部门ID: {} 未关联任何员工，仍可保存", dept.getId());
        } else {
            // 校验关联员工是否存在
            List<Employee> addedEmps = employeeDao.findByIds(newEmpIds);
            if (addedEmps.size() != newEmpIds.size()) {
                List<Integer> notExistEmps = newEmpIds.stream()
                        .filter(empId -> addedEmps.stream().noneMatch(e -> e.getId().equals(empId)))
                        .collect(Collectors.toList());
                throw new RuntimeException("选中的员工ID: " + notExistEmps + " 不存在，请检查");
            }

            // 同步迁移员工至新部门（清理原部门关联）
            addedEmps.forEach(emp -> {
                Integer oldDeptId = emp.getDepId(); // 员工原所属部门ID
                Integer newDeptId = dept.getId();   // 新部门ID

                // 3.1 更新员工的部门ID为新部门
                employeeDao.updateDepId(emp.getId(), newDeptId);
                log.info("员工ID: {} - 从原部门ID: {} 迁移到新部门ID: {}", emp.getId(), oldDeptId, newDeptId);

                // 3.2 清理原部门关联（移除员工、置空原部门负责人）
                if (oldDeptId != null) {
                    Department oldDept = departmentDao.findById(oldDeptId);
                    if (oldDept != null) {
                        // 从原部门empList移除该员工
                        List<Map<String, Integer>> oldEmpList = oldDept.getEmpList();
                        if (oldEmpList != null) {
                            oldEmpList.removeIf(empMap -> emp.getId().equals(empMap.get("empId")));
                            oldDept.setEmpList(oldEmpList);

                            // 若该员工是原部门负责人，置空原部门负责人ID
                            if (emp.getId().equals(oldDept.getManagerId())) {
                                oldDept.setManagerId(null);
                                log.info("原部门ID: {} - 负责人（员工ID: {}）已迁移，负责人置空", oldDeptId, emp.getId());
                            }

                            departmentDao.update(oldDept);
                            log.info("原部门ID: {} - 已从员工列表中移除员工ID: {}", oldDeptId, emp.getId());
                        }
                    } else {
                        log.warn("员工ID: {} 的原部门ID: {} 不存在，无需清理", emp.getId(), oldDeptId);
                    }
                }
            });
        }

        // 4. 校验负责人合法性，保存部门
        validateManagerInEmpList(dept.getManagerId(), newEmpIds, dept.getId());
        departmentDao.save(dept);
        log.info("新增部门ID: {} 成功（部门名称：{}）", dept.getId(), dept.getDepName());
    }

    /**
     * 更新部门（含员工关联变更、跨部门迁移同步、负责人校验）
     * 核心逻辑：
     * 1. 转换empIds为empList格式；
     * 2. 校验部门存在性，提取新旧员工ID列表；
     * 3. 处理移除员工（置空部门ID）、新增员工（同步迁移+清理原部门）；
     * 4. 校验负责人合法性，最终更新部门
     * @param newDept 待更新的部门实体（含主键ID、新关联员工/负责人信息）
     */
    @Override
    @Transactional
    public void updateDepartment(Department newDept) {
        Integer deptId = newDept.getId();
        log.info("开始更新部门ID: {}", deptId);

        // 1. 转换empIds为empList格式（适配数据库存储结构）
        if (newDept.getEmpIds() != null && !newDept.getEmpIds().isEmpty()) {
            List<Map<String, Integer>> empList = newDept.getEmpIds().stream()
                    .map(empId -> {
                        Map<String, Integer> map = new HashMap<>();
                        map.put("empId", empId);
                        return map;
                    })
                    .collect(Collectors.toList());
            newDept.setEmpList(empList);
        } else {
            newDept.setEmpList(new ArrayList<>());
        }

        // 2. 校验部门是否存在
        Department oldDept = departmentDao.findById(deptId);
        if (oldDept == null) {
            log.error("部门ID: {} 不存在，更新失败", deptId);
            throw new RuntimeException("部门ID:" + deptId + " 不存在");
        }

        // 3. 提取新旧员工ID列表，对比变更
        List<Integer> oldEmpIds = getEmpIdsFromList(oldDept.getEmpList());
        List<Integer> newEmpIds = getEmpIdsFromList(newDept.getEmpList());
        log.info("部门ID: {} - 旧员工ID列表: {}，新员工ID列表: {}", deptId, oldEmpIds, newEmpIds);

        // 4. 处理被移除的员工（置空其部门ID）
        List<Integer> removedEmpIds = oldEmpIds.stream()
                .filter(empId -> !newEmpIds.contains(empId))
                .collect(Collectors.toList());
        if (!removedEmpIds.isEmpty()) {
            removedEmpIds.forEach(empId -> employeeDao.updateDepId(empId, null));
            log.info("部门ID: {} 已移除员工: {}，其部门ID已置空", deptId, removedEmpIds);
        }

        // 5. 处理新增的员工（同步迁移+清理原部门关联）
        List<Integer> addedEmpIds = newEmpIds.stream()
                .filter(empId -> !oldEmpIds.contains(empId))
                .collect(Collectors.toList());
        if (!addedEmpIds.isEmpty()) {
            // 校验新增员工是否存在
            List<Employee> addedEmps = employeeDao.findByIds(addedEmpIds);
            if (addedEmps.size() != addedEmpIds.size()) {
                List<Integer> notExistEmps = addedEmpIds.stream()
                        .filter(empId -> addedEmps.stream().noneMatch(e -> e.getId().equals(empId)))
                        .collect(Collectors.toList());
                throw new RuntimeException("新增员工ID: " + notExistEmps + " 不存在");
            }

            // 同步迁移员工至当前部门（清理原部门关联）
            addedEmps.forEach(emp -> {
                Integer oldEmpDeptId = emp.getDepId(); // 员工原所属部门ID
                Integer currentDeptId = deptId;        // 当前编辑的部门ID

                // 5.1 更新员工的部门ID为当前部门
                employeeDao.updateDepId(emp.getId(), currentDeptId);
                log.info("员工ID: {} - 从原部门ID: {} 迁移到新部门ID: {}", emp.getId(), oldEmpDeptId, currentDeptId);

                // 5.2 清理原部门关联（非当前部门时）
                if (oldEmpDeptId != null && !oldEmpDeptId.equals(currentDeptId)) {
                    Department oldEmpDept = departmentDao.findById(oldEmpDeptId);
                    if (oldEmpDept != null) {
                        // 从原部门empList移除该员工
                        List<Map<String, Integer>> oldEmpDeptEmpList = oldEmpDept.getEmpList();
                        if (oldEmpDeptEmpList != null) {
                            oldEmpDeptEmpList.removeIf(empMap -> emp.getId().equals(empMap.get("empId")));
                            oldEmpDept.setEmpList(oldEmpDeptEmpList);

                            // 若该员工是原部门负责人，置空原部门负责人ID
                            if (emp.getId().equals(oldEmpDept.getManagerId())) {
                                oldEmpDept.setManagerId(null);
                                log.info("原部门ID: {} - 负责人（员工ID: {}）已迁移，负责人置空", oldEmpDeptId, emp.getId());
                            }

                            departmentDao.update(oldEmpDept);
                            log.info("原部门ID: {} - 已从员工列表中移除员工ID: {}", oldEmpDeptId, emp.getId());
                        }
                    } else {
                        log.warn("员工ID: {} 的原部门ID: {} 不存在，无需清理", emp.getId(), oldEmpDeptId);
                    }
                }
            });
        }

        // 6. 校验负责人合法性，更新部门
        validateManagerInEmpList(newDept.getManagerId(), newEmpIds, deptId);
        departmentDao.update(newDept);
        log.info("部门ID: {} 更新完成", deptId);
    }

    /**
     * 删除部门（同步置空关联员工的部门ID）
     * 核心逻辑：
     * 1. 校验部门存在性；
     * 2. 批量置空该部门下所有员工的depId；
     * 3. 最终删除部门实体
     * @param deptId 待删除部门的主键ID
     */
    @Override
    @Transactional
    public void deleteDepartment(Integer deptId) {
        log.info("开始删除部门ID: {}", deptId);

        // 1. 校验部门是否存在
        Department department = departmentDao.findById(deptId);
        if (department == null) {
            log.error("部门ID: {} 不存在，删除失败", deptId);
            throw new RuntimeException("部门ID:" + deptId + " 不存在");
        }

        // 2. 提取部门关联的员工ID，批量置空其部门ID
        List<Integer> empIds = getEmpIdsFromList(department.getEmpList());
        if (!empIds.isEmpty()) {
            employeeDao.batchUpdateDepIdToNull(empIds);
            log.info("部门ID: {} 关联的 {} 名员工已置空部门ID", deptId, empIds.size());
        } else {
            log.info("部门ID: {} 无关联员工，无需更新员工", deptId);
        }

        // 3. 最终删除部门
        departmentDao.delete(deptId);
        log.info("部门ID: {} 删除完成", deptId);
    }

    /**
     * 部门分页/全量查询（支持名称模糊匹配）
     * 核心逻辑：
     * 1. 区分全量查询（pageSize=-1）和分页查询；
     * 2. 调用DAO层查询原始部门数据；
     * 3. 统一转换为DepartmentDTO（含负责人姓名、员工简要信息）；
     * 4. 封装分页结果返回（兼容前端分页控件）
     * @param searchKey 部门名称关键词（可为空）
     * @param pageNum 当前页码（全量查询时传1）
     * @param pageSize 页大小（全量查询时传-1）
     * @return 分页结果对象（含DTO列表、分页参数、总条数）
     */
    @Override
    public Page<DepartmentDTO> listDepartments(String searchKey, int pageNum, int pageSize) {
        // 1. 区分全量查询和分页查询
        Page<Department> deptPage;
        if (pageSize == -1) { // 全量查询：封装为Page对象兼容前端
            List<Department> deptList = departmentDao.findByDepNameLikeIgnoreCase(searchKey);
            deptPage = new PageImpl<>(deptList, PageRequest.of(0, deptList.size()), deptList.size());
        } else { // 分页查询：调用DAO层分页方法
            deptPage = departmentDao.findByDepNameLikeWithPage(searchKey, pageNum, pageSize);
        }

        // 2. 统一转换为DepartmentDTO（包含负责人、员工简要信息）
        List<DepartmentDTO> dtoList = deptPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 3. 封装分页结果返回
        return new PageImpl<>(dtoList, deptPage.getPageable(), deptPage.getTotalElements());
    }

    // ==================== 私有工具方法 ====================

    /**
     * 生成自增部门ID
     * 逻辑：查询所有部门的最大ID，无数据时从1开始，有数据则最大ID+1
     * @return 新的部门自增ID
     */
    private Integer generateDeptId() {
        log.info("开始生成部门自增ID");

        // 1. 查询所有部门数据
        List<Department> allDepts = departmentDao.findAll();
        log.info("查询到所有部门数量：{}", allDepts.size());

        // 2. 提取有效部门ID，计算最大值
        Integer maxId = allDepts.stream()
                .map(Department::getId)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

        // 3. 生成新ID（无数据返回1，有数据返回maxId+1）
        Integer newDeptId = (maxId == null) ? 1 : maxId + 1;
        log.info("现有最大部门ID：{}，生成新ID：{}", maxId, newDeptId);

        return newDeptId;
    }

    /**
     * 从部门empList中提取员工ID列表
     * @param empList 部门关联的员工Map列表（格式：[{"empId": 1}, ...]）
     * @return 员工ID列表，空/无效时返回空列表
     */
    private List<Integer> getEmpIdsFromList(List<Map<String, Integer>> empList) {
        if (empList == null || empList.isEmpty()) {
            return new ArrayList<>();
        }
        return empList.stream()
                .map(empMap -> empMap.get("empId"))
                .filter(empId -> empId != null)
                .collect(Collectors.toList());
    }

    /**
     * 校验部门负责人合法性（必须是本部门员工）
     * @param managerId 负责人ID（可为空）
     * @param empIds 部门关联的员工ID列表
     * @param deptId 部门ID（用于日志/异常提示）
     */
    private void validateManagerInEmpList(Integer managerId, List<Integer> empIds, Integer deptId) {
        if (managerId == null) {
            log.warn("部门ID: {} 负责人为null，跳过校验（建议设置负责人）", deptId);
            return;
        }
        if (!empIds.contains(managerId)) {
            log.error("部门ID: {} 负责人ID: {} 不在员工列表: {} 中，校验失败", deptId, managerId, empIds);
            throw new RuntimeException("部门负责人ID:" + managerId + " 必须是本部门员工");
        }
        log.info("部门ID: {} 负责人ID: {} 校验通过（在员工列表中）", deptId, managerId);
    }

    /**
     * 旧版DTO转换方法（保留兼容）
     * @param dept 部门实体
     * @return 转换后的DepartmentDTO
     */
    private DepartmentDTO convertDepartmentToDTO(Department dept) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(dept.getId());
        dto.setDepName(dept.getDepName());
        dto.setManagerId(dept.getManagerId());

        // 补充负责人姓名
        if (dept.getManagerId() != null) {
            Employee manager = employeeDao.findById(dept.getManagerId());
            dto.setManagerName(manager != null ? manager.getEmpName() : "未知负责人");
        } else {
            dto.setManagerName("未设置");
        }

        // 补充员工简要信息
        List<DepartmentDTO.EmpSimpleDTO> empSimpleList = new ArrayList<>();
        if (dept.getEmpList() != null && !dept.getEmpList().isEmpty()) {
            List<Integer> empIds = getEmpIdsFromList(dept.getEmpList());
            List<Employee> emps = employeeDao.findByIds(empIds);

            empSimpleList = emps.stream()
                    .map(emp -> {
                        DepartmentDTO.EmpSimpleDTO simpleDTO = new DepartmentDTO.EmpSimpleDTO();
                        simpleDTO.setId(emp.getId());
                        simpleDTO.setEmpName(emp.getEmpName());
                        return simpleDTO;
                    })
                    .collect(Collectors.toList());
        }
        dto.setEmpList(empSimpleList);

        return dto;
    }

    /**
     * 通用DTO转换方法（整合最优逻辑）
     * @param dept 部门实体
     * @return 转换后的DepartmentDTO（含负责人姓名、员工简要信息）
     */
    private DepartmentDTO convertToDTO(Department dept) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(dept.getId());
        dto.setDepName(dept.getDepName());

        // 1. 补充负责人姓名
        if (dept.getManagerId() != null) {
            Employee manager = employeeDao.findById(dept.getManagerId());
            dto.setManagerName(manager != null ? manager.getEmpName() : "未设置");
        } else {
            dto.setManagerName("未设置");
        }

        // 2. 补充员工简要信息（优先用empIds，兼容getEmployeesByDeptId）
        List<Integer> empIds = dept.getEmpIds() != null ? dept.getEmpIds() : new ArrayList<>();
        List<Employee> empList = empIds.isEmpty() ? getEmployeesByDeptId(dept.getId()) : employeeDao.findByIds(empIds);

        List<DepartmentDTO.EmpSimpleDTO> empSimpleList = empList.stream().map(emp -> {
            DepartmentDTO.EmpSimpleDTO empSimple = new DepartmentDTO.EmpSimpleDTO();
            empSimple.setId(emp.getId());
            empSimple.setEmpName(emp.getEmpName());
            return empSimple;
        }).collect(Collectors.toList());
        dto.setEmpList(empSimpleList);

        return dto;
    }
}