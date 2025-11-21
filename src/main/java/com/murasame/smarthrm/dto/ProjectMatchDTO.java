package com.murasame.smarthrm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
public class ProjectMatchDTO implements Serializable {
    private Integer projId;
    private String projName;
    private Integer projStatus;

    /**
     * 辅助函数 作用：
     * "项目ID,项目名称,状态" → List<ProjectMatchDTO> */
    public static List<ProjectMatchDTO> fromString(String src) {
        if (src == null || src.isBlank()) return List.of();
        return Arrays.stream(src.split(","))
                .map(s -> s.split(":"))
                .map(a -> new ProjectMatchDTO(
                        Integer.valueOf(a[0]),
                        a.length > 1 ? a[1] : null,
                        a.length > 2 ? Integer.valueOf(a[2]) : null
                ))
                .toList();
    }
}