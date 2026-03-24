package com.pharmacy.prescription.messaging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
@Service
public class PgmqService {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  public PgmqService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) { this.jdbcTemplate = jdbcTemplate; this.objectMapper = objectMapper; }
  public long sendMessage(String queue, Object payload) {
    try {
      String json = objectMapper.writeValueAsString(payload);
      Long id = jdbcTemplate.queryForObject("SELECT pgmq.send(?::text, ?::jsonb)", Long.class, queue, json);
      return id == null ? -1 : id;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid payload", e);
    }
  }
  public List<Map<String,Object>> readMessages(String queue, int vt, int limit) {
    return jdbcTemplate.queryForList("SELECT * FROM pgmq.read(?::text, ?, ?)", queue, vt, limit);
  }
  public boolean deleteMessage(String queue, long id) {
    Boolean deleted = jdbcTemplate.queryForObject("SELECT pgmq.delete(?::text, ?)", Boolean.class, queue, id);
    return Boolean.TRUE.equals(deleted);
  }
}
