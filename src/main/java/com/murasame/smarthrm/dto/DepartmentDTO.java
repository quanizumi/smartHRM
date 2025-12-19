package com.murasame.smarthrm.dto;
//林 202512.19

import lombok.Data;

import java.util.List;

/**
 * 部门数据传输对象（DTO）
 * 用于部门列表页展示，封装部门基础信息、负责人信息及下属员工简要信息
 */
@Data
public class DepartmentDTO {
    // 部门ID（对应Department实体的主键）
    private Integer id;
    // 部门名称
    private String depName;
    // 部门负责人ID（关联Employee实体的主键）
    private Integer managerId;
    // 部门负责人姓名（关联员工表查询后填充，非数据库直接存储字段）
    private String managerName;
    // 部门下属员工简要信息列表（嵌套EmpSimpleDTO）
    private List<EmpSimpleDTO> empList;

    /**
     * 员工简要信息嵌套DTO
     * 仅封装员工ID和姓名，用于列表页轻量化展示，避免冗余字段
     */
    @Data
    public static class EmpSimpleDTO {
        // 员工ID（对应Employee实体的主键）
        private Integer id;
        // 员工姓名
        private String empName;
    }
}