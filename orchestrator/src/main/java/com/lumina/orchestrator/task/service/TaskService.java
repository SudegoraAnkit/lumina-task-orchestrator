package com.lumina.orchestrator.task.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.lumina.orchestrator.messaging.event.TaskCreatedEvent;
import com.lumina.orchestrator.messaging.producer.TaskProducer;
import com.lumina.orchestrator.task.dto.TaskRequest;
import com.lumina.orchestrator.task.dto.TaskResponse;
import com.lumina.orchestrator.task.entity.TaskEntity;
import com.lumina.orchestrator.task.entity.TaskStatus;
import com.lumina.orchestrator.task.repository.TaskRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskProducer taskProducer;

    public TaskResponse createTask(TaskRequest request) {

        TaskEntity task = TaskEntity.builder()
                .taskType(request.taskType())
                .payload(request.payload())
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TaskEntity saved = taskRepository.save(task);

        TaskCreatedEvent event = new TaskCreatedEvent(
                saved.getId(),
                saved.getTaskType(),
                saved.getPayload()
        );

        taskProducer.publishTask(event);

        return new TaskResponse(
                saved.getId(),
                saved.getStatus()
        );
    }
}
