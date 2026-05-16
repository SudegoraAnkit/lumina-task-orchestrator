package com.lumina.orchestrator.task.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lumina.orchestrator.task.entity.TaskEntity;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
}
