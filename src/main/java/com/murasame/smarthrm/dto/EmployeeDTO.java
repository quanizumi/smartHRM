package com.murasame.smarthrm.dto;
//林 202512.19

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmployeeDTO {
    // 分组标记：仅编辑时校验id非空
    public interface Update {}

    // 新增时id可为空，编辑时必须非空
    @NotNull(groups = Update.class, message = "员工ID不能为空")
    private Integer id;

    @NotBlank(message = "员工姓名不能为空")
    private String name; // 员工姓名

    //@NotBlank(message = "所属部门不能为空")
    private String department; // 所属部门ID（前端传递字符串，后端转Integer）

    private String skills; // 技能列表（格式："1:4,2:5"）

    private String projects; // 参与项目列表（格式："1,2"）

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    //@NotNull(message = "加入时间不能为空")
    private LocalDateTime joinDate;

    // 关联变更字段（更新项目/任务/培训关联）
    private List<Integer> newProjectIds;    // 更新后参与的项目ID列表（全量）
    private List<Integer> newManagerTaskIds;// 更新后负责的任务ID列表（全量）
    private List<Integer> newTrainingIds;   // 更新后参与的培训ID列表（全量）
}