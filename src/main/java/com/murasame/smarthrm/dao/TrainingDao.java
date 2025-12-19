package com.murasame.smarthrm.dao;
//林 2025.12.19

import com.murasame.smarthrm.entity.Training;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 培训数据访问层（DAO）
 * 基于MongoTemplate实现培训实体的基础查询、关联查询（按员工ID查参与培训）及更新操作
 */
@Repository
public class TrainingDao {

    // 注入MongoTemplate，用于操作MongoDB数据库
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据培训ID查询单个培训信息
     * @param id 培训主键ID（对应MongoDB文档的_id字段）
     * @return 匹配的Training实体，无匹配则返回null
     */
    public Training findById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Training.class);
    }

    /**
     * 查询所有培训信息
     * 核心用途：为前端下拉选择框提供全量培训列表，支持培训关联选择场景
     * @return 所有培训的List集合，无数据则返回空列表
     */
    public List<Training> findAll() {
        return mongoTemplate.findAll(Training.class);
    }

    /**
     * 根据员工ID查询该员工参与的所有培训
     * 匹配规则：通过elemMatch匹配培训memberList嵌套列表中包含该员工ID的培训
     * @param empId 员工主键ID
     * @return 该员工参与的培训列表，无匹配则返回空列表
     */
    public List<Training> findByMemberEmpId(Integer empId) {
        Query query = new Query(Criteria.where("members").is(empId));
        return mongoTemplate.find(query, Training.class);
    }

    /**
     * 更新培训信息
     * 包含培训名称、关联技能ID、参与员工列表等核心字段的更新
     * @param training 待更新的培训对象（必须包含主键ID）
     */
    public void update(Training training) {
        Query query = new Query(Criteria.where("_id").is(training.get_id()));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .set("trainName", training.getTrainName())
                .set("skillId", training.getSkillId())
                .set("members", training.getMembers());
        mongoTemplate.updateFirst(query, update, Training.class);
    }
}