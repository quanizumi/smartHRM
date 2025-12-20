package com.murasame.smarthrm.dao;

import com.murasame.smarthrm.entity.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Task数据访问层
 * Spring Data MongoDB Repository接口
 */
@Repository
public interface TaskRepo extends MongoRepository<Task, Integer> {
    /**
     * 根据项目ID查找所有任务
     */
    List<Task> findByProjId(Integer projId);

    /**
     * 根据项目ID和任务状态查找任务
     */
    List<Task> findByProjIdAndTaskStatus(Integer projId, Integer taskStatus);

    /**
     * 根据负责人ID查找任务
     */
    List<Task> findByManagerId(Integer managerId);

    /**
     * 检查项目是否存在任务
     */
    boolean existsByProjId(Integer projId);
}