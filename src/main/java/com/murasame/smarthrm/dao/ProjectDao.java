package com.murasame.smarthrm.dao;
//林 2025.12.19

import com.murasame.smarthrm.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 项目数据访问层（DAO）
 * 基于MongoTemplate实现项目实体的基础查询、关联查询（按员工ID查参与项目）及更新操作
 */
@Repository
public class ProjectDao {

    // 注入MongoTemplate，用于操作MongoDB数据库
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据项目ID查询单个项目信息
     * @param id 项目主键ID（对应MongoDB文档的_id字段）
     * @return 匹配的Project实体，无匹配则返回null
     */
    public Project findById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Project.class);
    }

    /**
     * 查询所有项目信息
     * @return 所有项目的List集合，无数据则返回空列表
     */
    public List<Project> findAll() {
        return mongoTemplate.findAll(Project.class);
    }

    /**
     * 根据员工ID查询该员工参与的所有项目
     * 匹配规则：通过elemMatch匹配项目members嵌套列表中包含该员工ID的项目
     * @param empId 员工主键ID
     * @return 该员工参与的项目列表，无匹配则返回空列表
     */
    public List<Project> findByMemberEmpId(Integer empId) {
        Query query = new Query(Criteria.where("members").elemMatch(Criteria.where("empId").is(empId)));
        return mongoTemplate.find(query, Project.class);
    }

    /**
     * 更新项目信息
     * 包含项目名称、参与成员、所需技能、项目状态、开始时间等核心字段的更新
     * @param project 待更新的项目对象（必须包含主键ID）
     */
    public void update(Project project) {
        Query query = new Query(Criteria.where("_id").is(project.getId()));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .set("projName", project.getProjName())
                .set("members", project.getMembers())
                .set("reqSkill", project.getReqSkill())
                .set("projStatus", project.getProjStatus())
                .set("startDate", project.getStartDate());
        mongoTemplate.updateFirst(query, update, Project.class);
    }
}