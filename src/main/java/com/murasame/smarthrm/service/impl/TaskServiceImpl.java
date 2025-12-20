package com.murasame.smarthrm.service.impl;

import com.murasame.smarthrm.dao.TaskRepo;
import com.murasame.smarthrm.entity.Task;
import com.murasame.smarthrm.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务服务实现类
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepo taskRepo;

    @Override
    public List<Task> getTasksByProjectId(Integer projId) {
        if (projId == null) {
            return List.of();
        }
        return taskRepo.findByProjId(projId);
    }

    @Override
    public List<Task> getTasksByProjectIdAndStatus(Integer projId, Integer taskStatus) {
        if (projId == null || taskStatus == null) {
            return List.of();
        }
        return taskRepo.findByProjIdAndTaskStatus(projId, taskStatus);
    }

    @Override
    public List<Task> getTasksByManagerId(Integer managerId) {
        if (managerId == null) {
            return List.of();
        }
        return taskRepo.findByManagerId(managerId);
    }

    @Override
    public Task createTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("任务信息不能为空");
        }

        // 检查项目ID是否有效
        if (task.getProjId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }

        // 生成新的任务ID（如果需要）
        if (task.get_id() == null) {
            // 获取当前最大ID + 1
            List<Task> allTasks = taskRepo.findAll();
            Integer maxId = allTasks.stream()
                    .map(Task::get_id)
                    .filter(id -> id != null)
                    .max(Integer::compareTo)
                    .orElse(0);
            task.set_id(maxId + 1);
        }

        // 设置默认状态
        if (task.getTaskStatus() == null) {
            task.setTaskStatus(0); // 默认未完成
        }

        return taskRepo.save(task);
    }

    @Override
    public Task updateTask(Task task) {
        if (task == null || task.get_id() == null) {
            throw new IllegalArgumentException("任务ID和任务信息不能为空");
        }

        // 检查任务是否存在
        Task existingTask = taskRepo.findById(task.get_id()).orElse(null);
        if (existingTask == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        return taskRepo.save(task);
    }

    @Override
    public boolean deleteTask(Integer taskId) {
        if (taskId == null) {
            return false;
        }

        // 检查任务是否存在
        if (!taskRepo.existsById(taskId)) {
            return false;
        }

        try {
            taskRepo.deleteById(taskId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Task getTaskById(Integer taskId) {
        if (taskId == null) {
            return null;
        }
        return taskRepo.findById(taskId).orElse(null);
    }

    @Override
    public boolean hasTasks(Integer projId) {
        if (projId == null) {
            return false;
        }
        return taskRepo.existsByProjId(projId);
    }
}