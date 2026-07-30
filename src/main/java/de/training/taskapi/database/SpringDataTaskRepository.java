package de.training.taskapi.database;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTaskRepository extends JpaRepository<TaskEntity, Long> {
}
