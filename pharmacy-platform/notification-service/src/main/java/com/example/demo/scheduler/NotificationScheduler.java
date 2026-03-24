package com.example.demo.scheduler;

import com.example.demo.model.NotificationMessage;
import com.example.demo.service.NotificationProcessor;
import com.example.demo.service.PgmqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled cron job that polls the pgmq 'notifications' queue
 * and dispatches messages to the appropriate notification channel.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final PgmqService pgmqService;
    private final NotificationProcessor notificationProcessor;

    @Value("${notifications.batch-size:10}")
    private int batchSize;

    @Value("${notifications.visibility-timeout:30}")
    private int visibilityTimeout;

    /**
     * Runs every 15 seconds by default. Reads messages from the pgmq queue,
     * processes each one, and archives successfully handled messages.
     *
     * Cron expression can be overridden via the
     * 'notifications.cron' application property.
     */
    @Scheduled(cron = "${notifications.cron:*/15 * * * * *}")
    public void pollNotificationQueue() {
        log.debug("Polling pgmq 'notifications' queue...");

        List<NotificationMessage> messages = pgmqService.readMessages(batchSize, visibilityTimeout);

        if (messages.isEmpty()) {
            log.debug("No messages in queue.");
            return;
        }

        log.info("Read {} message(s) from 'notifications' queue", messages.size());

        for (NotificationMessage message : messages) {
            try {
                notificationProcessor.process(message);
                pgmqService.archiveMessage(message.getMsgId());
                log.info("Successfully processed and archived message {}", message.getMsgId());
            } catch (Exception e) {
                log.error("Failed to process message {} — it will become visible again after timeout",
                        message.getMsgId(), e);
                // Message remains in queue; pgmq visibility timeout will make it
                // available for retry after the configured timeout period.
            }
        }
    }
}
