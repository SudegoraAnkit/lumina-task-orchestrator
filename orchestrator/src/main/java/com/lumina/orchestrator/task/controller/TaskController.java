package com.lumina.orchestrator.task.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.lumina.orchestrator.task.dto.TaskRequest;
import com.lumina.orchestrator.task.dto.TaskResponse;
import com.lumina.orchestrator.task.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid @RequestBody TaskRequest request
    ) {

        return taskService.createTask(request);
    }
}
