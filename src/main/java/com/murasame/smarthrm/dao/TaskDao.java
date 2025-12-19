package com.murasame.smarthrm.dao;
//林 2025.12.19

import com.murasame.smarthrm.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务数据访问层（DAO）
 * 基于MongoTemplate实现任务实体的基础查询、关联查询（按负责人ID）及更新操作，支撑任务管理相关业务
 */
@Repository
public class TaskDao {

    // 注入MongoTemplate，用于操作MongoDB数据库
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据任务ID查询单个任务信息
     * @param id 任务主键ID（对应MongoDB文档的_id字段）
     * @return 匹配的Task实体，无匹配则返回null
     */
    public Task findById(Integer id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, Task.class);
    }

    /**
     * 查询所有任务信息
     * 核心用途：为前端下拉选择框提供全量任务列表，支持任务关联选择场景
     * @return 所有任务的List集合，无数据则返回空列表
     */
    public List<Task> findAll() {
        // 无需条件，直接查询Task集合中所有数据
        return mongoTemplate.findAll(Task.class);
    }

    /**
     * 根据负责人ID查询该员工负责的所有任务
     * @param managerId 负责人（员工）主键ID
     * @return 该员工负责的任务列表，无匹配则返回空列表
     */
    public List<Task> findByManagerId(Integer managerId) {
        Query query = new Query(Criteria.where("managerId").is(managerId));
        return mongoTemplate.find(query, Task.class);
    }

    /**
     * 更新任务信息
     * 包含项目ID、任务名称、负责人ID、任务状态等核心字段的更新
     * @param task 待更新的任务对象（必须包含主键ID）
     */
    public void update(Task task) {
        Query query = new Query(Criteria.where("_id").is(task.get_id()));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .set("projId", task.getProjId())
                .set("taskName", task.getTaskName())
                .set("managerId", task.getManagerId())
                .set("taskStatus", task.getTaskStatus());
        mongoTemplate.updateFirst(query, update, Task.class);
    }
}