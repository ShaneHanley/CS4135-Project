package com.example.demo.scheduler;

import com.example.demo.model.NotificationMessage;
import com.example.demo.service.NotificationProcessor;
import com.pharmacy.messaging.PgmqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

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
    private final ObjectMapper objectMapper;

    @Value("${notifications.batch-size:10}")
    private int batchSize;

    @Value("${notifications.visibility-timeout:30}")
    private int visibilityTimeout;

    @Value("${notifications.max-retries:5}")
    private int maxRetries;

    @Value("${notifications.dead-letter-queue:dead_letter_notifications}")
    private String deadLetterQueue;

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

        List<Map<String, Object>> rows = pgmqService.readMessages("notifications", visibilityTimeout, batchSize);
        List<NotificationMessage> messages = rows.stream().map(row -> {
            try {
                Object msgObj = row.get("message");
                NotificationMessage msg = objectMapper.readValue(msgObj.toString(), NotificationMessage.class);
                msg.setMsgId(((Number) row.get("msg_id")).longValue());
                return msg;
            } catch (Exception e) {
                log.error("Failed to parse message: {}", row, e);
                return null;
            }
        }).filter(java.util.Objects::nonNull).toList();

        if (messages.isEmpty()) {
            log.debug("No messages in queue.");
            return;
        }

        log.info("Read {} message(s) from 'notifications' queue", messages.size());

        for (NotificationMessage message : messages) {
            Long msgId = message.getMsgId();
            try {
                if (message.getRetryCount() > maxRetries) {
                    pgmqService.sendMessage(deadLetterQueue, message);
                    pgmqService.archiveMessage("notifications", msgId);
                    log.warn("Moved message {} to dead-letter queue '{}' due to retryCount={}",
                        msgId, deadLetterQueue, message.getRetryCount());
                    continue;
                }
                if (notificationProcessor.isStale(message)) {
                    pgmqService.archiveMessage("notifications", msgId);
                    log.warn("Discarded stale notification message {} for prescription {}",
                        msgId, message.getPrescriptionId());
                    continue;
                }
                notificationProcessor.process(message);
                pgmqService.archiveMessage("notifications", msgId);
                log.info("Successfully processed and archived message {}", msgId);
            } catch (Exception e) {
                int retryCount = notificationProcessor.incrementRetryCount(message);
                String targetQueue = retryCount > maxRetries ? deadLetterQueue : "notifications";
                String actionPastTense = retryCount > maxRetries ? "dead-lettered" : "re-queued";
                String actionVerb = retryCount > maxRetries ? "dead-letter" : "re-queue";

                try {
                    pgmqService.sendMessage(targetQueue, message);
                    pgmqService.archiveMessage("notifications", msgId);
                    log.warn("{} message {} with retryCount={} to queue '{}'",
                        actionPastTense, msgId, retryCount, targetQueue);
                } catch (Exception sendError) {
                    log.error("Failed to {} message {}", actionVerb, msgId, sendError);
                }
            }
        }
    }
}
