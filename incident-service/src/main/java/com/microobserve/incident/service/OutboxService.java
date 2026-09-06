package com.microobserve.incident.service;

import com.microobserve.incident.model.OutboxEvent;
import com.microobserve.incident.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String aggregateId, String topic, Object event) {
        try {
            repository.save(new OutboxEvent(aggregateId, topic, objectMapper.writeValueAsString(event)));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unable to serialize outbox event", exception);
        }
    }
}
