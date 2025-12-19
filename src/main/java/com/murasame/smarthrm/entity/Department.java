package com.murasame.smarthrm.entity;
//林 2025.12.19
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "Department")
public class Department {

    @Id
    @Field("_id")
    private Integer id;          // 对应 _id

    @Field("depName")
    private String depName;

    @Field("managerId")
    private Integer managerId;   // 部门负责人ID

    @Field("empList")
    private List<Map<String, Integer>> empList;

    @Transient
    private List<Integer> empIds;//接收前端传递的员工ID数组（格式：[10,11,12]）

}
