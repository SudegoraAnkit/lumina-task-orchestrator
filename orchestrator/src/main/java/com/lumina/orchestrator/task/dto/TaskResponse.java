package com.lumina.orchestrator.task.dto;

import java.util.UUID;

import com.lumina.orchestrator.task.entity.TaskStatus;

public record TaskResponse(
        UUID taskId,
        TaskStatus status
) {
}
