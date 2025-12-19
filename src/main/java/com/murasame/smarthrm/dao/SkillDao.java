package com.murasame.smarthrm.dao;
//林 2025.12.19

import com.murasame.smarthrm.entity.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 技能数据访问层（DAO）
 * 基于MongoTemplate实现技能实体的基础查询，支撑技能存在性校验、前端下拉选择等业务场景
 */
@Repository
public class SkillDao {

    // 注入MongoTemplate，用于操作MongoDB数据库
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据技能ID查询单个技能信息
     * 核心用途：校验技能是否存在、获取技能名称/类型等基础信息
     * @param skillId 技能主键ID（对应MongoDB文档的_id字段）
     * @return 匹配的Skill实体，无匹配则返回null
     */
    public Skill findById(Integer skillId) {
        Query query = new Query(Criteria.where("_id").is(skillId));
        return mongoTemplate.findOne(query, Skill.class);
    }

    /**
     * 查询所有技能信息
     * 核心用途：为前端下拉选择框提供全量技能列表，支持技能关联选择场景
     * @return 所有技能的List集合，无数据则返回空列表
     */
    public List<Skill> findAll() {
        return mongoTemplate.findAll(Skill.class);
    }
}