package com.lumina.orchestrator.task.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String taskType;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private String payload;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}