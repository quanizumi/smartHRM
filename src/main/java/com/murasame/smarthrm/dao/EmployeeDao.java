package com.murasame.smarthrm.dao;
//林 2025.12.19

import com.murasame.smarthrm.dto.SkillMatchDTO;
import com.murasame.smarthrm.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
@Component
@RequiredArgsConstructor
public class EmployeeDao {

	private final MongoTemplate mongoTemplate;

	/*
	  匹配：skillList 里同时存在
	  key = skillId, value ≥ minLevel
	 */
	public List<Employee> findBySkillsRequired(List<SkillMatchDTO> reqs) {
		if (CollectionUtils.isEmpty(reqs)) return Collections.emptyList();

		/* 每个 req 转一个 elemMatch */
		List<Criteria> elemMatchCriterias = reqs.stream()
				.map(r -> Criteria.where("skillList").elemMatch(
						Criteria.where("skillId").is(r.getSkillId())
								.and("proficiency").gte(r.getMinLevel())
				))
				.toList();

		Query query = new Query(new Criteria().andOperator(elemMatchCriterias.toArray(new Criteria[0])));
		return mongoTemplate.find(query, Employee.class);
	}
	//修复报错
	public boolean existsById(Integer id) {
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.exists(query, Employee.class);
	}

    /**
     * 根据员工ID查询单个员工信息
     * @param id 员工主键ID（对应MongoDB文档的_id字段）
     * @return 匹配的Employee实体，无匹配则返回null
     */
    public Employee findById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Employee.class);
    }

    /**
     * 查询所有员工信息
     * @return 所有员工的List集合，无数据则返回空列表
     */
    public List<Employee> findAll() {
        return mongoTemplate.findAll(Employee.class);
    }

    /**
     * 批量查询员工信息（根据员工ID列表）
     * @param empIds 待查询的员工ID列表
     * @return 匹配的员工列表：ID列表为空时返回空列表，无匹配则返回空列表
     */
    public List<Employee> findByIds(List<Integer> empIds) {
        if (empIds.isEmpty()) return List.of();
        Query query = new Query(Criteria.where("_id").in(empIds));
        return mongoTemplate.find(query, Employee.class);
    }

    /**
     * 更新员工信息（支持不存在则插入）
     * 包含姓名、部门ID、技能列表、项目列表、入职时间等核心字段的更新
     * @param employee 待更新的员工对象（必须包含主键ID）
     */
    public void update(Employee employee) {
        Query query = new Query(Criteria.where("_id").is(employee.getId()));
        Update update = new Update()
                .set("empName", employee.getEmpName())
                .set("depId", employee.getDepId())
                .set("skillList", employee.getSkillList())  // 更新技能列表
                .set("projects", employee.getProjects())    // 更新项目列表
                //.set("trainingList", employee.getTrainingList()) // 同步更新培训列表（数据库中员工没有这个字段，所以不用更新）
                .set("joinDate", employee.getJoinDate());
        // 使用upsert：匹配到则更新第一条，未匹配到则插入新文档
        mongoTemplate.upsert(query, update, Employee.class);
    }

    /**
     * 更新单个员工的部门ID
     * @param empId 待更新的员工ID
     * @param newDeptId 新的部门ID
     */
    public void updateDepId(Integer empId, Integer newDeptId) {
        Query query = new Query(Criteria.where("_id").is(empId));
        Update update = new Update().set("depId", newDeptId);
        mongoTemplate.updateFirst(query, update, Employee.class);
    }

    /**
     * 批量更新员工部门ID为null（部门删除时调用）
     * @param empIds 待更新的员工ID列表
     */
    public void batchUpdateDepIdToNull(List<Integer> empIds) {
        if (empIds.isEmpty()) return;
        Query query = new Query(Criteria.where("_id").in(empIds));
        Update update = new Update().set("depId", null);
        mongoTemplate.updateMulti(query, update, Employee.class);
    }

    /**
     * 根据员工ID删除员工
     * @param id 待删除员工的主键ID
     */
    public void deleteById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, Employee.class);
    }

    /**
     * 根据员工姓名模糊查询（忽略大小写）
     * 匹配规则：姓名包含关键词即可，支持全模糊匹配
     * @param empName 员工姓名关键词（可为空，为空返回空列表）
     * @return 匹配的员工列表，无匹配则返回空列表
     */
    public List<Employee> findByEmpNameLikeIgnoreCase(String empName) {
        // 空值防护：关键词为空/仅空格时，直接返回空列表
        if (empName == null || empName.trim().isEmpty()) {
            return List.of();
        }

        // 构建模糊查询条件：empName字段包含关键词，"i"表示忽略大小写
        Criteria criteria = Criteria.where("empName")
                .regex(".*" + empName.trim() + ".*", "i");

        Query query = new Query(criteria);
        return mongoTemplate.find(query, Employee.class);
    }

    /**
     * 员工姓名模糊查询 + 分页查询
     * @param empName 员工姓名关键词（可为空，为空则查询所有员工）
     * @param pageNum 当前页码（前端传入从1开始，需转换为MongoDB的0起始页码）
     * @param pageSize 每页展示条数
     * @return 分页结果对象（包含当前页数据、总条数、分页参数）
     */
    public Page<Employee> findByEmpNameLikeWithPage(String empName, int pageNum, int pageSize) {
        // 1. 构建基础查询条件：姓名模糊匹配（忽略大小写）
        Query query = new Query();
        if (StringUtils.hasText(empName)) {
            Criteria criteria = Criteria.where("empName")
                    .regex(".*" + empName.trim() + ".*", "i");
            query.addCriteria(criteria);
        }

        // 2. 统计符合条件的总记录数（用于分页计算）
        long total = mongoTemplate.count(query, Employee.class);

        // 3. 设置分页参数：skip跳过前N条，limit限制每页条数（MongoDB页码从0开始）
        query.skip((pageNum - 1) * pageSize)
                .limit(pageSize);

        // 4. 查询当前页的员工数据
        List<Employee> employees = mongoTemplate.find(query, Employee.class);

        // 5. 封装为Spring Data的Page对象，返回分页结果
        return new PageImpl<>(employees, PageRequest.of(pageNum - 1, pageSize), total);
    }
}
