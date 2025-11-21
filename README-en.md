# SmartHRM

SmartHRM is a human resource management system based on Spring Boot, designed to help enterprises efficiently manage employee, skill, project, and training information.

## Features Overview

- **Employee Management**: Supports CRUD operations for employees.
- **Skill Matching**: Intelligent matching of suitable employees based on project skill requirements.
- **Project Matching**: Fuzzy Search project by name or involved employee's id.
- **Department and Project Management**: Manage organizational structure and project information.
- **Training Management**: Manage employee training records.

## Technology Stack

- Spring Boot
- MongoDB
- Thymeleaf (frontend template)
- Bootstrap & jQuery

## Module Description

### Core Modules

- **EmployeeController**: Manages employee information.
- **SkillMatchController**: Provides skill matching functionality, allowing search for suitable employees based on required project skills.
- **ProjectController**: Manages project information.
- **SkillController**: Manages skill information.
- **TrainingController**: Manages training information.

### Data Access Layer

- **EmployeeDao**: Database access class for employee information.
- **SkillRepo / ProjectRepo / DepartmentRepo**: Database access classes for skill, project, and department information, respectively.

### Entity Classes

- **Employee**: Employee entity containing name, department, skill list, and other information.
- **Skill**: Skill entity containing skill name, type, and other information.
- **Project**: Project entity containing project name, member list, required skills, and other information.
- **Department**: Department entity containing department name, manager ID, employee list, and other information.

### DTOs (Data Transfer Objects)

- **AddEmployeeDTO / ModEmployeeDTO**: Data transfer objects for adding and modifying employee information.
- **SkillMatchDTO**: Data transfer object for skill matching requests.

## Quick Start

### Prerequisites

- Java 17
- MongoDB
- Maven
- SpringBoot 3.x

### Configuration

1. Modify `application.yml` or use `application-dev.yml` / `application-prod.yml` to configure database connections and other settings.
2. Start the MongoDB service.

### Run the Project

```bash
mvn spring-boot:run
```

Visit `http://localhost:8080` to view the system homepage.

## Usage Instructions

- **Employee Management**: Access the `/employees/` route to add, modify, or delete employee records.
- **Skill Matching**: Access the `/skillmatch/` route, select required skills, and the system will automatically match eligible employees.
- **Project Management**: Access the `/projects/` route to manage project information.
- **Skill Management**: Access the `/skills/` route to manage skill information.
- **Project Matching**: Access the `/projectmatch/` route to fuzzy search project by name or involved employee's id.
- **Application.yml**: Modify the url of offline or online database. See details in the active .yml file.

## Contribution Guidelines

Contributions and suggestions are welcome! Please follow these steps:

1. Fork this repository.
2. Create a new branch (`git checkout -b feature/new-feature`).
3. Commit your changes (`git commit -m 'Add new feature'`).
4. Push to the remote branch (`git push origin feature/new-feature`).
5. Submit a Pull Request.

## License

Not available for now.
