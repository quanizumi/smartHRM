package com.murasame.smarthrm.dao;

import com.murasame.smarthrm.entity.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Project数据访问层
 * Spring Data MongoDB Repository接口
 */
@Repository
public interface ProjectRepo extends MongoRepository<Project, Integer> {
}