package com.murasame.smarthrm.dao;

import com.murasame.smarthrm.entity.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Employee数据访问层
 * Spring Data MongoDB Repository接口
 */
@Repository
public interface EmployeeRepo extends MongoRepository<Employee, Integer> {
}