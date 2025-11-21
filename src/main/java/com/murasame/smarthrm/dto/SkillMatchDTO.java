package com.murasame.smarthrm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
public class SkillMatchDTO  implements Serializable {
	private Integer skillId;
	private Integer minLevel;

	/**
	 * 辅助函数 作用：
	 * 1:3,2:5 → List<SkillMatchDTO> */
	public static List<SkillMatchDTO> fromString(String src) {
		if (src == null || src.isBlank()) return List.of();
		return Arrays.stream(src.split(","))
				.map(s -> s.split(":"))
				.map(a -> new SkillMatchDTO(Integer.valueOf(a[0]), Integer.valueOf(a[1])))
				.toList();
	}
}
