package com.murasame.smarthrm.service.impl;
//林 202512.19

import com.murasame.smarthrm.dao.*;
import com.murasame.smarthrm.dto.EmployeeDTO;
import com.murasame.smarthrm.entity.*;
import com.murasame.smarthrm.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工业务层实现类
 * 实现EmployeeService接口，处理员工CRUD及部门/项目/任务/培训的关联关系维护，
 * 通过@Transactional保证关联操作的事务一致性（要么全成功，要么全回滚）
 */
@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    // 日志组件，用于记录业务操作日志
    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    // 注入员工数据访问层，处理员工实体的数据库操作
    @Autowired
    private EmployeeDao employeeDao;
    // 注入部门数据访问层，处理部门关联操作
    @Autowired
    private DepartmentDao departmentDao;
    // 注入项目数据访问层，处理项目关联操作
    @Autowired
    private ProjectDao projectDao;
    // 注入任务数据访问层，处理任务关联操作
    @Autowired
    private TaskDao taskDao;
    // 注入培训数据访问层，处理培训关联操作
    @Autowired
    private TrainingDao trainingDao;
    // 注入技能数据访问层，处理技能校验操作
    @Autowired
    private SkillDao skillDao;

    /**
     * 查询所有员工信息（全量列表）
     * @return 所有员工的List集合，无数据则返回空列表
     */
    @Override
    public List<Employee> findAllEmployees() {
        return employeeDao.findAll();
    }

    /**
     * 根据员工ID查询单个员工信息
     * @param id 员工主键ID
     * @return 匹配的Employee实体，无匹配则返回null
     */
    @Override
    public Employee findEmployeeById(Integer id) {
        return employeeDao.findById(id);
    }

    /**
     * 查询所有员工信息（全量列表，与findAllEmployees功能一致，适配不同调用场景）
     * @return 所有员工的List集合，无数据则返回空列表
     */
    @Override
    public List<Employee> listAllEmployees() {
        return employeeDao.findAll();
    }

    /**
     * 新增员工（包含部门/项目/技能/培训关联关系初始化）
     * @param employee 待新增的员工实体（基础信息）
     * @param dto 员工修改DTO（封装关联的项目/培训ID列表、技能信息）
     */
    @Override
    public void saveEmployee(Employee employee, EmployeeDTO dto) {
        // 1. 生成员工自增ID（基于现有最大ID+1，无员工时从1开始）
        Integer newEmpId = generateEmpId();
        employee.setId(newEmpId);
        log.info("开始新增员工：生成自增ID = {}，员工姓名 = {}", newEmpId, employee.getEmpName());

        // 2. 解析并绑定技能（校验技能格式、存在性、熟练度范围）
        updateEmployeeSkills(employee, dto);

        // 3. 绑定部门关联（将员工添加到目标部门的员工列表）
        bindDepartment(employee);

        // 4. 绑定项目关联（将员工添加到选中项目的成员列表）
        bindProjects(employee.getId(), dto, employee);

        // 5. 绑定培训关联（将员工添加到选中培训的成员列表）
        bindTrainings(employee.getId(), dto, employee);

        // 6. 最终保存员工（MongoDB upsert：ID不存在则新增）
        employeeDao.update(employee);
        log.info("员工ID: {} 新增成功（含所有关联关系）", newEmpId);
    }

    /**
     * 更新员工信息（同步维护部门/项目/培训关联关系）
     * @param newEmployee 待更新的员工实体（包含主键ID）
     * @param dto 员工修改DTO（封装更新后的关联ID列表、技能信息）
     */
    @Override
    public void updateEmployee(Employee newEmployee, EmployeeDTO dto) {
        Integer empId = newEmployee.getId();
        Employee oldEmployee = employeeDao.findById(empId);
        if (oldEmployee == null) {
            throw new RuntimeException("员工ID:" + empId + " 不存在");
        }

        // 1. 更新员工技能列表（校验并解析技能信息）
        updateEmployeeSkills(newEmployee, dto);

        // 2. 处理部门关联变更（从旧部门移除、添加到新部门）
        handleDepartmentChange(oldEmployee, newEmployee);

        // 3. 处理项目关联变更（退出旧项目、加入新项目）
        handleProjectChange(empId, dto, newEmployee);
        Employee empAfterProject = employeeDao.findById(empId);

        // 4. 处理培训关联变更（退出旧培训、加入新培训）
        handleTrainingChange(empId, dto, newEmployee);

        // 5. 最终更新员工自身基础信息
        employeeDao.update(newEmployee);
    }

    /**
     * 删除员工（同步清理所有关联关系：部门/项目/任务/培训）
     * @param empId 待删除员工的主键ID
     */
    @Override
    public void deleteEmployee(Integer empId) {
        // 1. 校验员工是否存在
        Employee employee = employeeDao.findById(empId);
        if (employee == null) {
            throw new RuntimeException("员工ID:" + empId + " 不存在");
        }

        // 2. 清理部门关联：从部门移除员工，若为部门经理则置空经理ID
        handleDeptDelete(empId, employee.getDepId());

        // 3. 清理项目关联：从所有参与项目的成员列表移除员工
        handleProjectDelete(empId);

        // 4. 清理任务关联：置空所有该员工负责的任务的负责人ID
        // handleTaskChange(empId, dto);

        // 5. 清理培训关联：从所有参与培训的成员列表移除员工
        handleTrainingDelete(empId);

        // 6. 最终删除员工实体
        employeeDao.deleteById(empId);
    }

    /**
     * 根据员工姓名模糊查询员工列表
     * @param empName 员工姓名关键词（可为空）
     * @return 匹配的员工列表：关键词为空时返回所有员工，无匹配则返回空列表
     */
    @Override
    public List<Employee> findEmployeesByName(String empName) {
        if (StringUtils.hasText(empName)) {
            // 姓名不为空：执行姓名模糊查询（忽略大小写）
            return employeeDao.findByEmpNameLikeIgnoreCase(empName);
        } else {
            // 姓名为空：查询所有员工
            return employeeDao.findAll();
        }
    }

    /**
     * 员工分页查询（支持姓名模糊匹配）
     * @param empName 员工姓名关键词（可为空）
     * @param pageNum 当前页码（前端传入从1开始）
     * @param pageSize 每页展示条数
     * @return 分页结果对象（包含当前页数据、总条数、分页参数）
     */
    @Override
    public Page<Employee> listEmployeesWithPage(String empName, int pageNum, int pageSize) {
        // 调用DAO层分页查询方法，实现姓名模糊+分页
        return employeeDao.findByEmpNameLikeWithPage(empName, pageNum, pageSize);
    }

    // ==================== 核心业务私有方法（更新/新增关联处理） ====================

    /**
     * 解析并更新员工技能列表
     * 逻辑：
     * 1. 解析前端传递的技能字符串（格式：skillId:熟练度）；
     * 2. 校验技能格式、技能存在性、熟练度范围（1-5）；
     * 3. 去重后设置到员工实体，空值时清空技能列表
     * @param newEmployee 待更新的员工实体
     * @param dto 封装技能字符串的DTO
     */
    private void updateEmployeeSkills(Employee newEmployee, EmployeeDTO dto) {
        List<Map<String, Integer>> updatedSkillList = new ArrayList<>();
        String skillsStr = dto.getSkills();

        // 技能字符串非空时解析处理
        if (skillsStr != null && !skillsStr.trim().isEmpty()) {
            String[] skillArray = skillsStr.split(",");
            for (String skillItem : skillArray) {
                String[] skillParts = skillItem.split(":");

                // 格式校验：必须是 "数字:数字" 格式
                if (skillParts.length != 2
                        || !skillParts[0].matches("\\d+")
                        || !skillParts[1].matches("\\d+")) {
                    throw new RuntimeException("技能格式错误：" + skillItem + "，请按「技能ID:熟练度」格式输入（例：1:4），无需技能可留空");
                }

                Integer skillId = Integer.parseInt(skillParts[0]);
                Integer proficiency = Integer.parseInt(skillParts[1]);

                // 校验技能ID是否存在
                Skill existSkill = skillDao.findById(skillId);
                if (existSkill == null) {
                    throw new RuntimeException("技能ID:" + skillId + " 不存在，请选择系统中已有的技能，无需技能可留空");
                }

                // 校验熟练度范围（1-5）
                if (proficiency < 1 || proficiency > 5) {
                    throw new RuntimeException("技能「" + existSkill.getSkillName() + "」（ID:" + skillId + "）的熟练度需在1-5之间，无需技能可留空");
                }

                // 封装技能数据
                Map<String, Integer> skillMap = new HashMap<>();
                skillMap.put("skillId", skillId);
                skillMap.put("proficiency", proficiency);
                updatedSkillList.add(skillMap);
            }

            // 技能去重（按skillId去重，保留第一条）
            List<Map<String, Integer>> distinctSkillList = updatedSkillList.stream()
                    .collect(Collectors.toMap(
                            skill -> skill.get("skillId"),
                            skill -> skill,
                            (oldVal, newVal) -> oldVal
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());

            newEmployee.setSkillList(distinctSkillList);
        } else {
            // 技能字符串为空时，设置空列表
            newEmployee.setSkillList(new ArrayList<>());
        }
    }

    /**
     * 处理员工部门关联变更
     * 逻辑：
     * 1. 从旧部门移除员工，若为旧部门经理则置空经理ID；
     * 2. 将员工添加到新部门，避免重复添加
     * @param oldEmp 变更前的员工实体
     * @param newEmp 变更后的员工实体
     */
    private void handleDepartmentChange(Employee oldEmp, Employee newEmp) {
        Integer oldDepId = oldEmp.getDepId();
        Integer newDepId = newEmp.getDepId();

        if (!equals(oldDepId, newDepId)) {
            // 从旧部门移除员工
            if (oldDepId != null) {
                Department oldDept = departmentDao.findById(oldDepId);
                if (oldDept != null && oldDept.getEmpList() != null) {
                    oldDept.getEmpList().removeIf(empMap -> oldEmp.getId().equals(empMap.get("empId")));
                    // 若为旧部门经理，置空经理ID
                    if (oldEmp.getId().equals(oldDept.getManagerId())) {
                        oldDept.setManagerId(null);
                    }
                    departmentDao.update(oldDept);
                }
            }

            // 添加到新部门
            if (newDepId != null) {
                Department newDept = departmentDao.findById(newDepId);
                if (newDept == null) {
                    throw new RuntimeException("新部门ID:" + newDepId + " 不存在");
                }
                if (newDept.getEmpList() == null) {
                    newDept.setEmpList(new ArrayList<>());
                }
                // 避免重复添加
                boolean exists = newDept.getEmpList().stream()
                        .anyMatch(empMap -> newEmp.getId().equals(empMap.get("empId")));
                if (!exists) {
                    newDept.getEmpList().add(Map.of("empId", newEmp.getId()));
                    departmentDao.update(newDept);
                }
            }
        }
    }

    /**
     * 处理员工项目关联变更
     * 逻辑：
     * 1. 退出旧项目：从项目成员列表移除员工；
     * 2. 加入新项目：向项目成员列表添加员工；
     * 3. 同步更新员工实体的项目关联列表
     * @param empId 员工ID
     * @param dto 封装新项目ID列表的DTO
     * @param newEmployee 变更后的员工实体
     */
    private void handleProjectChange(Integer empId, EmployeeDTO dto, Employee newEmployee) {
        // 1. 获取旧项目ID列表（员工原本关联的项目）
        List<Integer> oldProjectIds = getOldProjectIds(empId);
        // 2. 获取新项目ID列表（员工现在选择的项目）
        List<Integer> newProjectIds = dto.getNewProjectIds() == null ? new ArrayList<>() : dto.getNewProjectIds();

        // 3. 退出旧项目
        for (Integer projId : oldProjectIds) {
            if (!newProjectIds.contains(projId)) {
                Project project = getValidProject(projId);
                if (project.getMembers() == null) {
                    continue;
                }
                boolean removed = project.getMembers().removeIf(member -> empId.equals(member.getEmpId()));
                if (removed) {
                    projectDao.update(project);
                }
            }
        }

        // 4. 加入新项目
        for (Integer projId : newProjectIds) {
            if (!oldProjectIds.contains(projId)) {
                Project project = getValidProject(projId);
                if (project.getMembers() == null) {
                    project.setMembers(new ArrayList<>());
                }
                boolean alreadyInProject = project.getMembers().stream()
                        .anyMatch(member -> empId.equals(member.getEmpId()));
                if (!alreadyInProject) {
                    Project.Member member = new Project.Member();
                    member.setEmpId(empId);
                    project.getMembers().add(member);
                    projectDao.update(project);
                }
            }
        }

        // 5. 同步更新员工的项目关联列表
        List<Map<String, Integer>> employeeProjects = newProjectIds.stream()
                .map(projId -> Map.of("projId", projId))
                .collect(Collectors.toList());
        newEmployee.setProjects(employeeProjects);
    }

    /**
     * 处理员工培训关联变更
     * 逻辑：
     * 1. 退出旧培训：从培训成员列表移除员工；
     * 2. 加入新培训：向培训成员列表添加员工；
     * 3. 同步更新员工实体的培训关联列表
     * @param empId 员工ID
     * @param dto 封装新培训ID列表的DTO
     * @param newEmployee 变更后的员工实体
     */
    private void handleTrainingChange(Integer empId, EmployeeDTO dto, Employee newEmployee) {
        List<Integer> oldTrainingIds = getOldTrainingIds(empId);
        List<Integer> newTrainingIds = dto.getNewTrainingIds() == null ? new ArrayList<>() : dto.getNewTrainingIds();

        log.info("员工ID: {} - 旧培训ID列表: {}", empId, oldTrainingIds);
        log.info("员工ID: {} - 新培训ID列表: {}", empId, newTrainingIds);

        // 退出旧培训
        for (Integer trainId : oldTrainingIds) {
            if (!newTrainingIds.contains(trainId)) {
                Training training = getValidTraining(trainId);
                log.info("培训ID: {} - 从数据库读取的memberList: {}", trainId, training.getMembers());

                if (training.getMembers() == null) {
                    training.setMembers(new ArrayList<>());
                    log.info("培训ID: {} - 数据库memberList为null，初始化空列表", trainId);
                    continue;
                }

                boolean removed = training.getMembers().removeIf(memberId -> empId.equals(memberId));
                if (removed) {
                    trainingDao.update(training);
                    log.info("培训ID: {} - 成功从memberList删除员工ID: {}", trainId, empId);
                } else {
                    log.info("培训ID: {} - memberList中无员工ID: {}，无需删除", trainId, empId);
                }
            }
        }

        // 加入新培训
        for (Integer trainId : newTrainingIds) {
            if (!oldTrainingIds.contains(trainId)) {
                Training training = getValidTraining(trainId);
                if (training.getMembers() == null) {
                    training.setMembers(new ArrayList<>());
                    log.info("培训ID: {} - 数据库memberList为null，初始化空列表", trainId);
                }

                boolean alreadyInTraining = training.getMembers().stream()
                        .anyMatch(memberId -> empId.equals(memberId));
                if (!alreadyInTraining) {
                    training.getMembers().add(empId);
                    trainingDao.update(training);
                    log.info("培训ID: {} - 成功向memberList添加员工ID: {}", trainId, empId);
                } else {
                    log.info("培训ID: {} - 员工ID: {} 已在memberList中，无需重复添加", trainId, empId);
                }
            }
        }

        // 同步更新员工的培训关联列表
        List<Map<String, Integer>> employeeTrainings = newTrainingIds.stream()
                .map(trainId -> Map.of("trainId", trainId))
                .collect(Collectors.toList());
        newEmployee.setTrainingList(employeeTrainings);
        log.info("员工ID: {} - 最终关联的培训列表: {}", empId, employeeTrainings);
    }

    // ==================== 核心业务私有方法（删除关联处理） ====================

    /**
     * 清理员工的部门关联（删除员工时调用）
     * 逻辑：从部门员工列表移除员工，若为部门经理则置空经理ID
     * @param empId 员工ID
     * @param depId 员工所属部门ID
     */
    private void handleDeptDelete(Integer empId, Integer depId) {
        if (depId != null) {
            Department department = departmentDao.findById(depId);
            if (department != null) {
                // 从部门员工列表移除
                if (department.getEmpList() != null) {
                    department.getEmpList().removeIf(empMap -> empId.equals(empMap.get("empId")));
                }
                // 若为部门经理，置空经理ID
                if (empId.equals(department.getManagerId())) {
                    department.setManagerId(null);
                }
                departmentDao.update(department);
            }
        }
    }

    /**
     * 清理员工的项目关联（删除员工时调用）
     * 逻辑：从所有该员工参与的项目成员列表中移除员工
     * @param empId 员工ID
     */
    private void handleProjectDelete(Integer empId) {
        log.info("开始处理员工ID: {} 的项目关联删除", empId);

        // 查询所有包含该员工的项目
        List<Project> projects = projectDao.findByMemberEmpId(empId);
        if (projects.isEmpty()) {
            log.info("员工ID: {} 未关联任何项目，无需删除项目成员", empId);
            return;
        }

        // 遍历项目，移除员工ID
        for (Project project : projects) {
            Integer projId = project.getId();
            if (project.getMembers() == null) {
                project.setMembers(new ArrayList<>());
                log.info("项目ID: {} - members为null，初始化空列表", projId);
                continue;
            }

            boolean removed = project.getMembers().removeIf(member -> empId.equals(member.getEmpId()));
            if (removed) {
                projectDao.update(project);
                log.info("项目ID: {} - 成功从members中移除员工ID: {}", projId, empId);
            } else {
                log.info("项目ID: {} - members中无员工ID: {}，无需删除", projId, empId);
            }
        }

        log.info("员工ID: {} 的项目关联删除处理完成", empId);
    }

    /**
     * 清理员工的任务关联（删除员工时调用）
     * 逻辑：将该员工负责的所有任务的负责人ID置空
     * @param empId 员工ID
     */
    private void handleTaskDelete(Integer empId) {
        List<Task> tasks = taskDao.findByManagerId(empId);
        for (Task task : tasks) {
            task.setManagerId(null);
            taskDao.update(task);
        }
    }

    /**
     * 清理员工的培训关联（删除员工时调用）
     * 逻辑：从所有该员工参与的培训成员列表中移除员工
     * @param empId 员工ID
     */
    private void handleTrainingDelete(Integer empId) {
        log.info("开始处理员工ID: {} 的培训关联删除", empId);

        // 查询该员工参与的所有培训
        List<Training> trainings = trainingDao.findByMemberEmpId(empId);
        if (trainings.isEmpty()) {
            log.info("员工ID: {} 未关联任何培训，无需删除培训成员", empId);
            return;
        }

        // 遍历培训，移除员工ID
        for (Training training : trainings) {
            Integer trainId = training.get_id();
            String trainName = training.getTrainName();

            if (training.getMembers() == null) {
                training.setMembers(new ArrayList<>());
                log.info("培训ID: {}（{}）- memberList为null，初始化空列表", trainId, trainName);
                continue;
            }

            boolean removed = training.getMembers().removeIf(memberId -> empId.equals(memberId));
            if (removed) {
                trainingDao.update(training);
                log.info("培训ID: {}（{}）- 成功从memberList移除员工ID: {}", trainId, trainName, empId);
            } else {
                log.info("培训ID: {}（{}）- memberList中无员工ID: {}，无需删除", trainId, trainName, empId);
            }
        }

        log.info("员工ID: {} 的培训关联删除处理完成", empId);
    }

    // ==================== 新增专用私有方法 ====================

    /**
     * 生成员工自增ID
     * 逻辑：查询所有员工的最大ID，无员工时从1开始，有员工则最大ID+1
     * @return 新的员工ID
     */
    private Integer generateEmpId() {
        log.info("开始生成员工自增ID");
        // 查询所有员工，提取最大ID
        List<Employee> allEmps = employeeDao.findAll();
        Integer maxId = allEmps.stream()
                .map(Employee::getId)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
        // 生成新ID
        Integer newEmpId = (maxId == null) ? 1 : maxId + 1;
        log.info("现有最大员工ID：{}，生成新ID：{}", maxId, newEmpId);
        return newEmpId;
    }

    /**
     * 绑定新增员工的部门关联
     * 逻辑：将员工添加到目标部门的员工列表，校验部门存在性
     * @param employee 新增的员工实体
     */
    private void bindDepartment(Employee employee) {
        Integer depId = employee.getDepId();
        if (depId == null) {
            log.warn("员工ID: {} 未选择所属部门，跳过部门关联", employee.getId());
            return;
        }
        // 校验部门是否存在
        Department dept = departmentDao.findById(depId);
        if (dept == null) {
            throw new RuntimeException("部门ID:" + depId + " 不存在，无法关联");
        }
        // 初始化部门员工列表
        if (dept.getEmpList() == null) {
            dept.setEmpList(new ArrayList<>());
        }
        // 避免重复添加
        boolean exists = dept.getEmpList().stream()
                .anyMatch(empMap -> employee.getId().equals(empMap.get("empId")));
        if (!exists) {
            dept.getEmpList().add(Map.of("empId", employee.getId()));
            departmentDao.update(dept);
            log.info("员工ID: {} 已关联到部门ID: {}", employee.getId(), depId);
        }
    }

    /**
     * 绑定新增员工的项目关联
     * 逻辑：将员工添加到选中项目的成员列表，校验项目存在性，避免重复添加
     * @param empId 新增员工的ID
     * @param dto 封装新项目ID列表的DTO
     * @param employee 新增的员工实体
     */
    private void bindProjects(Integer empId, EmployeeDTO dto, Employee employee) {
        List<Integer> newProjectIds = dto.getNewProjectIds() == null ? new ArrayList<>() : dto.getNewProjectIds();
        if (newProjectIds.isEmpty()) {
            log.warn("员工ID: {} 未选择任何项目，跳过项目关联", empId);
            employee.setProjects(new ArrayList<>());
            return;
        }

        List<Map<String, Integer>> employeeProjects = new ArrayList<>();
        for (Integer projId : newProjectIds) {
            // 校验项目是否存在
            Project project = getValidProject(projId);
            // 初始化项目成员列表
            if (project.getMembers() == null) {
                project.setMembers(new ArrayList<>());
            }
            // 避免重复添加
            boolean alreadyIn = project.getMembers().stream()
                    .anyMatch(member -> empId.equals(member.getEmpId()));
            if (!alreadyIn) {
                Project.Member member = new Project.Member();
                member.setEmpId(empId);
                project.getMembers().add(member);
                projectDao.update(project);
                log.info("员工ID: {} 已加入项目ID: {}", empId, projId);
            }
            // 封装员工的项目列表
            employeeProjects.add(Map.of("projId", projId));
        }
        // 绑定到员工对象
        employee.setProjects(employeeProjects);
    }

    /**
     * 绑定新增员工的培训关联
     * 逻辑：将员工添加到选中培训的成员列表，校验培训存在性，避免重复添加
     * @param empId 新增员工的ID
     * @param dto 封装新培训ID列表的DTO
     * @param employee 新增的员工实体
     */
    private void bindTrainings(Integer empId, EmployeeDTO dto, Employee employee) {
        List<Integer> newTrainingIds = dto.getNewTrainingIds() == null ? new ArrayList<>() : dto.getNewTrainingIds();
        if (newTrainingIds.isEmpty()) {
            log.warn("员工ID: {} 未选择任何培训，跳过培训关联", empId);
            employee.setTrainingList(new ArrayList<>());
            return;
        }

        List<Map<String, Integer>> employeeTrainings = new ArrayList<>();
        for (Integer trainId : newTrainingIds) {
            // 校验培训是否存在
            Training training = getValidTraining(trainId);
            // 初始化培训成员列表
            if (training.getMembers() == null) {
                training.setMembers(new ArrayList<>());
            }
            // 避免重复添加
            boolean alreadyIn = training.getMembers().stream()
                    .anyMatch(memberId -> empId.equals(memberId));
            if (!alreadyIn) {
                training.getMembers().add(empId);
                trainingDao.update(training);
                log.info("员工ID: {} 已加入培训ID: {}", empId, trainId);
            }
            // 封装员工的培训列表
            employeeTrainings.add(Map.of("trainId", trainId));
        }
        // 绑定到员工对象
        employee.setTrainingList(employeeTrainings);
    }

    // ==================== 通用工具私有方法 ====================

    /**
     * 获取员工已参与的项目ID列表
     * @param empId 员工ID
     * @return 项目ID列表，无项目则返回空列表
     */
    private List<Integer> getOldProjectIds(Integer empId) {
        return projectDao.findByMemberEmpId(empId).stream()
                .map(Project::getId)
                .collect(Collectors.toList());
    }

    /**
     * 获取员工已负责的任务ID列表
     * @param empId 员工ID
     * @return 任务ID列表，无任务则返回空列表
     */
    private List<Integer> getOldTaskIds(Integer empId) {
        return taskDao.findByManagerId(empId).stream()
                .map(Task::get_id)
                .collect(Collectors.toList());
    }

    /**
     * 获取员工已参与的培训ID列表
     * @param empId 员工ID
     * @return 培训ID列表，无培训则返回空列表
     */
    private List<Integer> getOldTrainingIds(Integer empId) {
        return trainingDao.findByMemberEmpId(empId).stream()
                .map(Training::get_id)
                .collect(Collectors.toList());
    }

    /**
     * 比较两个Integer是否相等（处理null值）
     * @param a 第一个整数
     * @param b 第二个整数
     * @return 相等返回true，否则返回false
     */
    private boolean equals(Integer a, Integer b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 查询项目并校验存在性（减少重复代码）
     * @param projId 项目ID
     * @return 存在则返回Project实体，不存在则抛出异常
     */
    private Project getValidProject(Integer projId) {
        Project project = projectDao.findById(projId);
        if (project == null) {
            log.error("项目ID: {} 不存在", projId);
            throw new RuntimeException("项目ID:" + projId + " 不存在");
        }
        return project;
    }

    /**
     * 查询培训并校验存在性（减少重复代码）
     * @param trainId 培训ID
     * @return 存在则返回Training实体，不存在则抛出异常
     */
    private Training getValidTraining(Integer trainId) {
        Training training = trainingDao.findById(trainId);
        if (training == null) {
            log.error("培训ID: {} 不存在", trainId);
            throw new RuntimeException("培训ID:" + trainId + " 不存在");
        }
        return training;
    }
}