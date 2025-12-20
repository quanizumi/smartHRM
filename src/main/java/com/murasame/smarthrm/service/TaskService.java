package com.murasame.smarthrm.service;

import com.murasame.smarthrm.entity.Task;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 根据项目ID获取所有任务
     * @param projId 项目ID
     * @return 项目任务列表
     */
    List<Task> getTasksByProjectId(Integer projId);

    /**
     * 根据项目ID和任务状态获取任务
     * @param projId 项目ID
     * @param taskStatus 任务状态 (0-未完成, 1-已完成)
     * @return 指定状态的任务列表
     */
    List<Task> getTasksByProjectIdAndStatus(Integer projId, Integer taskStatus);

    /**
     * 根据负责人ID获取任务
     * @param managerId 负责人ID
     * @return 负责人任务列表
     */
    List<Task> getTasksByManagerId(Integer managerId);

    /**
     * 创建新任务
     * @param task 任务信息
     * @return 创建的任务
     */
    Task createTask(Task task);

    /**
     * 更新任务信息
     * @param task 任务信息
     * @return 更新后的任务
     */
    Task updateTask(Task task);

    /**
     * 删除任务
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteTask(Integer taskId);

    /**
     * 根据ID获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    Task getTaskById(Integer taskId);

    /**
     * 检查项目是否有任务
     * @param projId 项目ID
     * @return 是否有任务
     */
    boolean hasTasks(Integer projId);
}