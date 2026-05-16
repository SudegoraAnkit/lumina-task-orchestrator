package com.lumina.orchestrator.messaging.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.lumina.orchestrator.messaging.event.TaskCreatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private static final String TOPIC_NAME = "task-created";

    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;

    public void publishTask(TaskCreatedEvent event) {

        log.info("Publishing task event: {}", event);

        kafkaTemplate.send(
                TOPIC_NAME,
                event.taskId().toString(),
                event
        );
    }
}