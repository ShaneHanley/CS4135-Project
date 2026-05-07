package com.pharmacy.prescription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.messaging.PgmqService;
import com.pharmacy.prescription.entity.OutboxEvent;
import com.pharmacy.prescription.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {

    @Mock
    OutboxEventRepository outboxEventRepository;

    @Mock
    PgmqService pgmqService;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    OutboxPublisherService service;

    @Test
    void publishAndMark_sendsToPgmqAndSetsPublishedAt() throws Exception {
        OutboxEvent ev = new OutboxEvent();
        ev.setQueueName("prescription_created");
        ev.setPayload(objectMapper.writeValueAsString(Map.of("prescriptionId", "abc", "doctorId", "d1")));
        ev.setCreatedAt(Instant.now());

        service.publishAndMark(ev);

        verify(pgmqService).sendMessage(eq("prescription_created"), any(Map.class));
        ArgumentCaptor<OutboxEvent> cap = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(cap.capture());
        assertThat(cap.getValue().getPublishedAt()).isNotNull();
        assertThat(cap.getValue().getLastError()).isNull();
    }
}
