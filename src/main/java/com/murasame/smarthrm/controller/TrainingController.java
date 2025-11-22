package com.murasame.smarthrm.controller;

import com.murasame.smarthrm.dao.EmployeeDao;
import com.murasame.smarthrm.dao.SkillRepo;
import com.murasame.smarthrm.dao.TrainingRepo;
import com.murasame.smarthrm.entity.Training;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingRepo trainingRepo;
    private final SkillRepo skillRepo;
    private final EmployeeDao employeeDao;

    @PostMapping("/add")
    public ResponseEntity<String> addTraining(@RequestBody Training training) {
        if (training.getTrainName() == null || training.getTrainName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("错误：课程名称不能为空！");
        }
        if (trainingRepo.existsByTrainName(training.getTrainName())) {
            return ResponseEntity.badRequest().body("错误：课程 '" + training.getTrainName() + "' 已存在！");
        }

        // 校验关联数据
        String validResult = validateRelations(training);
        if (validResult != null) return ResponseEntity.badRequest().body(validResult);

        // 校验ID冲突
        if (training.get_id() != null && trainingRepo.existsById(training.get_id())) {
            return ResponseEntity.badRequest().body("错误：课程ID " + training.get_id() + " 已存在，请更换或留空！");
        }

        if (training.get_id() == null) {
            training.set_id((int) (System.currentTimeMillis() / 1000));
        }

        trainingRepo.save(training);
        return ResponseEntity.ok("成功：培训课程已发布！");
    }

    //  新增：修改接口
    @PostMapping("/update")
    public ResponseEntity<String> updateTraining(@RequestBody Training training) {
        if (training.get_id() == null || !trainingRepo.existsById(training.get_id())) {
            return ResponseEntity.badRequest().body("错误：未找到ID为 " + training.get_id() + " 的课程，无法修改！");
        }

        if (training.getTrainName() == null || training.getTrainName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("错误：课程名称不能为空！");
        }

        // 校验关联数据
        String validResult = validateRelations(training);
        if (validResult != null) return ResponseEntity.badRequest().body(validResult);

        trainingRepo.save(training);
        return ResponseEntity.ok("成功：培训课程信息已更新！");
    }

    // 辅助方法：校验关联ID是否存在
    private String validateRelations(Training training) {
        if (training.getSkillId() == null || !skillRepo.existsById(training.getSkillId())) {
            return "错误：关联的技能ID无效或不存在！";
        }
        if (training.getMembers() != null) {
            for (Integer empId : training.getMembers()) {
                if (!employeeDao.existsById(empId)) {
                    return "错误：员工ID (" + empId + ") 不存在！";
                }
            }
        }
        return null;
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteTraining(@PathVariable Integer id) {
        if (trainingRepo.existsById(id)) {
            trainingRepo.deleteById(id);
            return ResponseEntity.ok("成功：课程已删除");
        }
        return ResponseEntity.badRequest().body("错误：未找到该课程ID");
    }

    @GetMapping("/list")
    public Page<Training> listTrainings(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return trainingRepo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "_id")));
    }

    @GetMapping("/search")
    public List<Training> searchTrainings(@RequestParam String name) {
        return trainingRepo.findByTrainNameContaining(name);
    }

    @GetMapping("/bySkill/{skillId}")
    public List<Training> getTrainingBySkill(@PathVariable Integer skillId) {
        return trainingRepo.findBySkillId(skillId);
    }
}