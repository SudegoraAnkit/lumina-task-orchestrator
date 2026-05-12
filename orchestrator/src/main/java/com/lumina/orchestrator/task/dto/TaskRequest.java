package com.lumina.orchestrator.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskRequest(

        @NotBlank
        String taskType,

        @NotBlank
        String payload
) {
}
