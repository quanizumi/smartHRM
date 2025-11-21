package com.murasame.smarthrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 新增员工的数据传输对象（接收前端表单提交的参数）
 */
@Data // Lombok注解，自动生成getter、setter、toString等方法
public class AddEmployeeDTO {

    /**
     * 员工ID（必须为正整数，前端表单字段名需为"id"）
     */
    @NotNull(message = "员工ID不能为空")
    private Integer id;

    /**
     * 员工姓名（非空，长度1-20）
     */
    @NotBlank(message = "员工姓名不能为空")
    @Pattern(regexp = "^.{1,20}$", message = "姓名长度需在1-20个字符之间")
    private String name;

    /**
     * 部门ID（前端传递字符串形式的数字，后端转为Integer）
     */
    @NotBlank(message = "部门ID不能为空")
    @Pattern(regexp = "^\\d+$", message = "部门ID必须为数字")
    private String department;

    /**
     * 技能列表（格式：List<Map<Integer, Integer>>，每个Map代表{技能ID: 熟练度}）
     * 例如：[{1:4}, {2:3}] 表示技能ID=1（熟练度4）、技能ID=2（熟练度3）
     */
    private List<Map<Integer, Integer>> skills;

    /**
     * 项目列表（格式：List<Integer>，存储项目ID）
     * 例如：[1, 2] 表示参与项目ID=1和项目ID=2
     */
    private List<Integer> projects;

}