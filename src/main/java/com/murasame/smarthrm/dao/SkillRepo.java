package com.murasame.smarthrm.dao;

import com.murasame.smarthrm.entity.Skill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public interface SkillRepo extends MongoRepository<Skill, Integer> {
}
