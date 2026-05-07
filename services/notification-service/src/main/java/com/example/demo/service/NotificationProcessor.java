package com.example.demo.service;

import com.example.demo.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Processes notification messages by routing them to the appropriate
 * channel (SMS or Email) and formatting based on the notification type.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessor {

    private final TwilioSmsService smsService;
    private final TwilioEmailService emailService;
    private final PrescriptionStatusService prescriptionStatusService;

    /**
     * Processes a single notification message.
     * Routes to SMS or Email based on the channel, and formats the
     * content based on the type (received, processing, collect).
     */
    public void process(NotificationMessage message) {
        String channel = message.getChannel();
        String type = message.getType();

        log.info("Processing notification — channel: {}, type: {}, recipient: {}",
                channel, type, message.getRecipient());

        String formattedBody = formatBody(type, message.getBody());
        String subject = formatSubject(type, message.getSubject());

        switch (channel.toLowerCase()) {
            case "sms" -> sendSms(message.getRecipient(), formattedBody);
            case "email" -> sendEmail(message.getRecipient(), subject, formattedBody);
            default -> log.warn("Unknown channel '{}' for message id {}", channel, message.getMsgId());
        }
    }

    private void sendSms(String to, String body) {
        smsService.sendSms(to, body);
    }

    private void sendEmail(String to, String subject, String body) {
        emailService.sendEmail(to, subject, body);
    }

    /**
     * Returns true when the message is older than the latest prescription update.
     */
    public boolean isStale(NotificationMessage message) {
        if (message.getCreatedAt() == null) {
            return false;
        }
        String prescriptionId = message.getPrescriptionId();
        if (prescriptionId == null || prescriptionId.isBlank()) {
            return false;
        }

        return prescriptionStatusService.findPrescriptionUpdatedAt(prescriptionId)
            .map(updatedAt -> message.getCreatedAt().isBefore(updatedAt))
            .orElse(false);
    }

    /**
     * Increments retry count on failure and returns the new count.
     */
    public int incrementRetryCount(NotificationMessage message) {
        int next = message.getRetryCount() + 1;
        message.setRetryCount(next);
        return next;
    }

    /**
     * Formats the message body based on the notification type.
     */
    private String formatBody(String type, String rawBody) {
        if (rawBody == null) rawBody = "";

        return switch (type.toLowerCase()) {
            case "received" -> "📬 Your item has been received.\n\n" + rawBody;
            case "processing" -> "⚙️ Your item is currently being processed.\n\n" + rawBody;
            case "collect" -> "✅ Your item is ready for collection!\n\n" + rawBody;
            default -> rawBody;
        };
    }

    /**
     * Formats the email subject based on the notification type.
     * Falls back to a default subject if none is provided.
     */
    private String formatSubject(String type, String rawSubject) {
        if (rawSubject != null && !rawSubject.isBlank()) {
            return rawSubject;
        }

        return switch (type.toLowerCase()) {
            case "received" -> "Notification: Item Received";
            case "processing" -> "Notification: Item Processing";
            case "collect" -> "Notification: Ready for Collection";
            default -> "Notification";
        };
    }
}
