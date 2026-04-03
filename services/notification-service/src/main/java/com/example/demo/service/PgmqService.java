package com.example.demo.service;

import com.example.demo.model.NotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service that interacts with Supabase pgmq to read and archive/delete messages
 * from the 'notifications' queue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PgmqService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_NAME = "notifications";

    /**
     * Reads a batch of messages from the pgmq queue.
     *
     * @param batchSize   max number of messages to read
     * @param visibilityTimeout  seconds the message is hidden from other consumers
     * @return list of NotificationMessage objects
     */
    public List<NotificationMessage> readMessages(int batchSize, int visibilityTimeout) {
        List<NotificationMessage> messages = new ArrayList<>();

        try {
            // pgmq.read(queue_name, visibility_timeout, batch_size)
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM pgmq.read(?, ?, ?)",
                    QUEUE_NAME, visibilityTimeout, batchSize
            );

            for (Map<String, Object> row : rows) {
                try {
                    Long msgId = ((Number) row.get("msg_id")).longValue();
                    Object messageObj = row.get("message");
                    String messageJson;

                    if (messageObj instanceof String) {
                        messageJson = (String) messageObj;
                    } else {
                        // pgmq may return message as a PGobject or similar
                        messageJson = messageObj.toString();
                    }

                    NotificationMessage msg = objectMapper.readValue(messageJson, NotificationMessage.class);
                    msg.setMsgId(msgId);
                    messages.add(msg);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse message from queue: {}", row, e);
                }
            }
        } catch (Exception e) {
            log.error("Error reading messages from pgmq queue '{}'", QUEUE_NAME, e);
        }

        return messages;
    }

    /**
     * Archives a message after successful processing (keeps it in archive table).
     */
    public boolean archiveMessage(long msgId) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT pgmq.archive(?, ?)",
                    Boolean.class, QUEUE_NAME, msgId
            );
            log.debug("Archived message {} from queue '{}'", msgId, QUEUE_NAME);
            return true;
        } catch (Exception e) {
            log.error("Failed to archive message {} from queue '{}'", msgId, QUEUE_NAME, e);
            return false;
        }
    }

    /**
     * Deletes a message from the queue permanently (no archive).
     */
    public boolean deleteMessage(long msgId) {
        try {
            jdbcTemplate.queryForObject(
                    "SELECT pgmq.delete(?, ?)",
                    Boolean.class, QUEUE_NAME, msgId
            );
            log.debug("Deleted message {} from queue '{}'", msgId, QUEUE_NAME);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete message {} from queue '{}'", msgId, QUEUE_NAME, e);
            return false;
        }
    }
}
