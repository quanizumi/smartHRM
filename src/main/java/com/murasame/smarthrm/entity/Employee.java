package com.murasame.smarthrm.entity;
//林 2025.12.19
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 数据库实体
@Data
@Document(collection = "Employees") // 对应MongoDB中的集合名
public class Employee {
    @Id               // ← 告诉 Spring Data 这是主键
    @Field("_id")     // ← 强制映射文档字段 "_id"
    private Integer id;
    private String empName;
    private Integer depId;
    private List<Map<String, Integer>> skillList;  // 员工拥有技能: [{技能id, 熟练度}...]
    private List<Map<String, Integer>> projects;                 // 员工参与项目: [projId:项目id...]
    private LocalDateTime joinDate;                 // 加入时间
    private List<Map<String, Integer>> trainingList;

    private String deptName; // 临时部门名称（前端显示用）
    private String deptType; // 临时部门类型（用于前端样式）
}