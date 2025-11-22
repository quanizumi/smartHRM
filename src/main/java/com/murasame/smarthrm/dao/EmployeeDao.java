package com.murasame.smarthrm.dao;

import com.murasame.smarthrm.dto.SkillMatchDTO;
import com.murasame.smarthrm.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
@Component
@RequiredArgsConstructor
public class EmployeeDao {

	private final MongoTemplate mongoTemplate;

	/*
	  匹配：skillList 里同时存在
	  key = skillId, value ≥ minLevel
	 */
	public List<Employee> findBySkillsRequired(List<SkillMatchDTO> reqs) {
		if (CollectionUtils.isEmpty(reqs)) return Collections.emptyList();

		/* 每个 req 转一个 elemMatch */
		List<Criteria> elemMatchCriterias = reqs.stream()
				.map(r -> Criteria.where("skillList").elemMatch(
						Criteria.where("skillId").is(r.getSkillId())
								.and("proficiency").gte(r.getMinLevel())
				))
				.toList();

		Query query = new Query(new Criteria().andOperator(elemMatchCriterias.toArray(new Criteria[0])));
		return mongoTemplate.find(query, Employee.class);
	}
	//修复报错
	public boolean existsById(Integer id) {
		Query query = new Query(Criteria.where("_id").is(id));
		return mongoTemplate.exists(query, Employee.class);
	}
}
