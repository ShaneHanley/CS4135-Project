package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * Represents a notification message pulled from the pgmq 'notifications' queue.
 *
 * Expected JSON payload structure:
 * {
 *   "channel": "sms" | "email",
 *   "type": "received" | "processing" | "collect",
 *   "recipient": "+15551234567" (for SMS) or "user@example.com" (for email),
 *   "subject": "optional email subject",
 *   "body": "The message content",
 *   "prescriptionId": "uuid",
 *   "retryCount": 0,
 *   "createdAt": "2024-01-01T00:00:00Z"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationMessage {

    /** The delivery channel: "sms" or "email" */
    private String channel;

    /** The notification type: "received", "processing", or "collect" */
    private String type;

    /** The recipient phone number (for SMS) or email address (for email) */
    private String recipient;

    /** The email subject line (used for email channel only) */
    private String subject;

    /** The message body content */
    private String body;

    /** The prescription id associated with this notification (if applicable) */
    private String prescriptionId;

    /** Number of delivery attempts so far */
    private int retryCount;

    /** Timestamp for when the notification was created */
    private Instant createdAt;

    /** pgmq message id — populated after reading from the queue */
    @JsonProperty(value = "msg_id", access = JsonProperty.Access.WRITE_ONLY)
    private Long msgId;
}
