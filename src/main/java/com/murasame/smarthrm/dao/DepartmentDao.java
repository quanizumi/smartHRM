package com.murasame.smarthrm.dao;
//林 2025.12.19

import com.murasame.smarthrm.entity.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 部门数据访问层（DAO）
 * 基于MongoTemplate实现部门实体的CRUD操作及高级查询（模糊查询、分页查询）
 */
@Repository
public class DepartmentDao {

    // 注入MongoTemplate，用于操作MongoDB数据库
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据部门ID查询单个部门信息
     * @param id 部门主键ID（对应MongoDB文档的_id字段）
     * @return 匹配的Department实体，无匹配则返回null
     */
    public Department findById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Department.class);
    }

    /**
     * 查询所有部门信息
     * @return 所有部门的List集合，无数据则返回空列表
     */
    public List<Department> findAll() {
        return mongoTemplate.findAll(Department.class);
    }

    /**
     * 保存部门信息（支持新增）
     * - 若部门对象的_id（主键）不存在 → 执行新增操作
     * - 若_id已存在 → 覆盖更新（Service层需控制新增时生成唯一_id，避免误更新）
     * @param department 待保存的部门对象（新增时需包含生成的主键ID）
     */
    public void save(Department department) {
        // 调用MongoTemplate的save方法，自动处理新增/更新逻辑
        mongoTemplate.save(department);
    }

    /**
     * 更新部门信息（根据部门ID更新名称、负责人、员工列表）
     * @param department 待更新的部门对象（必须包含主键ID）
     */
    public void update(Department department) {
        Query query = new Query(Criteria.where("_id").is(department.getId()));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .set("depName", department.getDepName())
                .set("managerId", department.getManagerId())
                .set("empList", department.getEmpList());
        mongoTemplate.updateFirst(query, update, Department.class);
    }

    /**
     * 根据部门ID删除部门
     * @param deptId 待删除部门的主键ID
     */
    public void delete(Integer deptId) {
        Query query = new Query(Criteria.where("_id").is(deptId));
        mongoTemplate.remove(query, Department.class);
    }

    /**
     * 根据部门名称模糊查询（忽略大小写）
     * @param searchKey 部门名称关键词（可为空）
     * @return 匹配的部门列表：关键词为空时返回所有部门，无匹配则返回空列表
     */
    public List<Department> findByDepNameLikeIgnoreCase(String searchKey) {
        // 空值防护：关键词为空/仅空格时，返回所有部门
        if (searchKey == null || searchKey.trim().isEmpty()) {
            return mongoTemplate.findAll(Department.class);
        }

        // 构建模糊查询条件：匹配包含关键词的部门名称，忽略大小写
        Criteria criteria = Criteria.where("depName")
                .regex(".*" + searchKey.trim() + ".*", "i"); // "i" 表示忽略大小写

        Query query = new Query(criteria);
        return mongoTemplate.find(query, Department.class);
    }

    /**
     * 部门名称模糊查询 + 分页查询
     * @param searchKey 部门名称关键词（可为空）
     * @param pageNum 当前页码（前端传入从1开始，需转换为MongoDB的0起始页码）
     * @param pageSize 每页展示条数
     * @return 分页结果对象（包含当前页数据、总条数、分页参数）
     */
    public Page<Department> findByDepNameLikeWithPage(String searchKey, int pageNum, int pageSize) {
        // 1. 构建基础查询条件
        Query query = new Query();
        if (searchKey != null && !searchKey.trim().isEmpty()) {
            Criteria criteria = Criteria.where("depName")
                    .regex(".*" + searchKey.trim() + ".*", "i");
            query.addCriteria(criteria);
        }

        // 2. 统计符合条件的总记录数（用于分页计算）
        long total = mongoTemplate.count(query, Department.class);

        // 3. 设置分页参数：skip跳过前N条，limit限制每页条数（MongoDB页码从0开始）
        query.skip((pageNum - 1) * pageSize)
                .limit(pageSize);

        // 4. 查询当前页的部门数据
        List<Department> departments = mongoTemplate.find(query, Department.class);

        // 5. 封装为Spring Data的Page对象，返回分页结果
        return new PageImpl<>(departments, PageRequest.of(pageNum - 1, pageSize), total);
    }
}