package com.multichunk.demo.services;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.logging.Logger;

@Service
public class WebhookStreamProducer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final Logger logger = Logger.getLogger(WebhookStreamProducer.class.getName());

    public WebhookStreamProducer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void produceEvent(Map<String, Object> eventData) {
        RecordId id = redisTemplate
                .opsForStream()
                .add(
                        StreamRecords
                                .newRecord()
                                .in("minio_uploads")
                                .ofMap(eventData)
                );

        logger.info("Produced event to Redis stream 'minio_uploads' with ID: " + id.getValue());
    }
}
