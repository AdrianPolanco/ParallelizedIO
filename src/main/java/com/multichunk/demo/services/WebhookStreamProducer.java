package com.multichunk.demo.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.logging.Logger;

@Service
public class WebhookStreamProducer {

    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Logger logger = Logger.getLogger(WebhookStreamProducer.class.getName());

    public WebhookStreamProducer(@Qualifier("redisJsonTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void produceEvent(Map<String, Object> eventData) throws JsonProcessingException {
        RecordId id = redisTemplate
                .opsForStream()
                .add(
                        StreamRecords
                                .newRecord()
                                .in(streamKey)
                                .ofMap(Map.of("payload", eventData))
                );

        logger.info("Produced event to Redis stream 'minio_uploads' with ID: " + id.getValue());
    }
}
