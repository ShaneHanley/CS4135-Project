package com.pharmacy.prescription.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.messaging.PgmqService;
import com.pharmacy.prescription.entity.OutboxEvent;
import com.pharmacy.prescription.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final PgmqService pgmqService;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(
            OutboxEventRepository outboxEventRepository, PgmqService pgmqService, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.pgmqService = pgmqService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishAndMark(OutboxEvent event) {
        try {
            Map<String, Object> payload =
                    objectMapper.readValue(event.getPayload(), new TypeReference<Map<String, Object>>() {});
            pgmqService.sendMessage(event.getQueueName(), payload);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            event.setLastError(msg.length() > 2000 ? msg.substring(0, 2000) : msg);
        }
        outboxEventRepository.save(event);
    }
}
