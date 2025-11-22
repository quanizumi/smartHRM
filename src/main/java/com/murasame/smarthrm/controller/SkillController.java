package com.murasame.smarthrm.controller;

import com.murasame.smarthrm.dao.SkillRepo;
import com.murasame.smarthrm.entity.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillRepo skillRepo;

    @PostMapping("/add")
    public ResponseEntity<String> addSkill(@RequestBody Skill skill) {
        // 1. 校验名称
        if (skill.getSkillName() == null || skill.getSkillName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("错误：技能名称不能为空！");
        }
        // 2. 校验重名
        if (skillRepo.existsBySkillName(skill.getSkillName())) {
            return ResponseEntity.badRequest().body("错误：技能 '" + skill.getSkillName() + "' 已存在！");
        }

        // 3. 校验手动输入的ID是否冲突
        if (skill.get_id() != null && skillRepo.existsById(skill.get_id())) {
            return ResponseEntity.badRequest().body("错误：技能ID " + skill.get_id() + " 已存在，请更换或留空自动生成！");
        }

        // 4. 自动生成ID
        if (skill.get_id() == null) {
            skill.set_id((int) (System.currentTimeMillis() / 1000));
        }

        skillRepo.save(skill);
        return ResponseEntity.ok("成功：技能添加完成！");
    }

    // === 新增：修改接口 ===
    @PostMapping("/update")
    public ResponseEntity<String> updateSkill(@RequestBody Skill skill) {
        // 1. 检查ID是否存在
        if (skill.get_id() == null || !skillRepo.existsById(skill.get_id())) {
            return ResponseEntity.badRequest().body("错误：未找到ID为 " + skill.get_id() + " 的技能，无法修改！");
        }

        // 2. 校验名称非空
        if (skill.getSkillName() == null || skill.getSkillName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("错误：技能名称不能为空！");
        }

        skillRepo.save(skill);
        return ResponseEntity.ok("成功：技能信息已更新！");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteSkill(@PathVariable Integer id) {
        if (skillRepo.existsById(id)) {
            skillRepo.deleteById(id);
            return ResponseEntity.ok("成功：技能已删除");
        }
        return ResponseEntity.badRequest().body("错误：未找到该技能ID");
    }

    @GetMapping("/list")
    public Page<Skill> listSkills(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return skillRepo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "_id")));
    }

    @GetMapping("/search")
    public List<Skill> searchSkills(@RequestParam String name) {
        return skillRepo.findBySkillNameContaining(name);
    }

    @GetMapping("/{id}")
    public Skill getSkill(@PathVariable Integer id) {
        return skillRepo.findById(id).orElse(null);
    }
}