package com.lumina.orchestrator.messaging.event;

import java.util.UUID;

public record TaskCreatedEvent(
        UUID taskId,
        String taskType,
        String payload
) {
}
