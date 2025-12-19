package com.murasame.smarthrm.config;
//林2025.12.19
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * WebMvc配置类
 * 核心功能：注册自定义类型转换器，处理前端传递的"yyyy-MM-dd"格式日期字符串转换为LocalDateTime类型（东八区当天0点），
 * 解决前端日期输入与后端LocalDateTime类型的适配问题
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 时区常量：东八区（Asia/Shanghai），统一日期转换的时区基准
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    // 日期格式化器：适配前端传递的"yyyy-MM-dd"格式日期字符串
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 注册自定义类型转换器
     * 核心逻辑：将前端"yyyy-MM-dd"格式的字符串转换为LocalDateTime（东八区当天0点），兼容空值/空字符串
     * @param registry 类型转换器注册器
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 注册String → LocalDateTime转换器
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            // 空值/空字符串直接返回null，避免转换异常
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            // 1. 解析字符串为LocalDate（适配yyyy-MM-dd格式）
            LocalDate localDate = LocalDate.parse(source, DATE_FORMATTER);
            // 2. 转换为LocalDateTime（东八区当天0点，保证时区一致性）
            return localDate.atStartOfDay(DEFAULT_ZONE).toLocalDateTime();
        });
    }
}