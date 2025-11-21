# SmartHRM

SmartHRM 是一个基于 Spring Boot 的人力资源管理系统，旨在帮助企业高效管理其员工、技能、项目及培训等信息。

## 多语言目录 Multi-Language Index
- README-cn.md - Here
- README-en.md - [README-en.md](https://github.com/Murasame-Chyan/smartHRM/blob/master/README-en.md)。

## 功能概览

- **员工管理**：支持员工的增删改查操作。
- **技能匹配**：根据项目需求技能，智能匹配合适的员工。
- **项目检索**：根据项目名字段或参与员工id模糊搜索项目。
- **部门与项目管理**：管理组织架构和项目信息。
- **培训管理**：管理员工培训记录。

## 技术栈

- Spring Boot
- MongoDB
- Thymeleaf (前端模板)
- Bootstrap & jQuery

## 模块说明

### 核心模块

- **EmployeeController**：员工信息管理。
- **SkillMatchController**：技能匹配功能，支持根据项目所需技能搜索合适员工。
- **ProjectController**：项目信息管理。
- **SkillController**：技能信息管理。
- **TrainingController**：培训信息管理。

### 数据访问层

- **EmployeeDao**：员工信息的数据库访问类。
- **SkillRepo / ProjectRepo / DepartmentRepo**：分别用于技能、项目、部门信息的数据库访问。

### 实体类

- **Employee**：员工实体，包含姓名、所属部门、技能列表等信息。
- **Skill**：技能实体，包含技能名称、类型等信息。
- **Project**：项目实体，包含项目名称、成员列表、所需技能等信息。
- **Department**：部门实体，包含部门名称、经理ID、员工列表等信息。

### DTO（数据传输对象）

- **AddEmployeeDTO / ModEmployeeDTO**：用于员工新增与修改的数据传输对象。
- **SkillMatchDTO**：用于技能匹配请求的数据传输对象。

## 快速开始

### 环境要求

- Java 17
- MongoDB
- Maven
- SpringBoot 3.x

### 配置

1. 修改 `application.yml` 或使用 `application-dev.yml` / `application-prod.yml` 配置数据库连接等信息。
2. 启动 MongoDB 服务。

### 启动项目

```bash
mvn spring-boot:run
```

访问 `http://localhost:8080` 查看系统首页。

## 使用说明

- **员工管理**：访问 `/employees/` 路由，可进行员工的添加、修改、删除操作。
- **技能匹配**：访问 `/skillmatch/` 路由，选择所需技能后，系统将自动匹配符合条件的员工。
- **项目管理**：访问 `/projects/` 路由，管理项目信息。
- **技能管理**：访问 `/skills/` 路由，管理技能信息。
- **项目匹配管理**：访问 `/projectmatch/` 路由，以项目名或参与员工id模糊搜索项目详情。
- **application.yml**：设置本地数据库或云端数据库url，详见生效yml文件。

## 贡献指南

欢迎贡献代码或提出建议！请遵循以下步骤：

1. Fork 本仓库。
2. 创建新分支 (`git checkout -b feature/new-feature`)。
3. 提交更改 (`git commit -m 'Add new feature'`)。
4. 推送至远程分支 (`git push origin feature/new-feature`)。
5. 提交 Pull Request。

## 许可证

暂无
