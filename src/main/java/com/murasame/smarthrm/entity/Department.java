package com.murasame.smarthrm.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Data
@Document(collection = "Department")
public class Department {

	@Id
	private Integer id;          // 对应 _id

	@Field("depName")
	private String depName;

	@Field("managerId")
	private Integer managerId;   // 部门负责人ID

	@Field("empList")
	private List<Map<String, Integer>> empList;

//	/* ===== 嵌套对象：仅存 empId ===== */
//	@Data
//	public static class Member {
//		@Field("empId")
//		private Integer empId;
//	}
}