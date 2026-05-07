package com.pharmacy.prescription.scheduler;

import com.pharmacy.prescription.entity.OutboxEvent;
import com.pharmacy.prescription.repository.OutboxEventRepository;
import com.pharmacy.prescription.service.OutboxPublisherService;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherService outboxPublisherService;

    public OutboxPublisherScheduler(OutboxEventRepository outboxEventRepository, OutboxPublisherService outboxPublisherService) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPublisherService = outboxPublisherService;
    }

    @Scheduled(fixedDelayString = "2000")
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> batch = outboxEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : batch) {
            outboxPublisherService.publishAndMark(event);
        }
    }
}
