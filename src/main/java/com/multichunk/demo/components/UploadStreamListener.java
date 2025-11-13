package com.multichunk.demo.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class UploadStreamListener implements StreamListener<String, ObjectRecord<String, Object>> {
    private static final Logger logger = Logger.getLogger(UploadStreamListener.class.getName());
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis-streams.uploads.consumer-group}")
    private String consumerGroup;

    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;

    public UploadStreamListener(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onMessage(ObjectRecord<String, Object> message) {
        try {
            logger.info("Processing message ID: " + message.getId() + " Body: " + message.getValue());// Aquí va tu lógica de validación...
            // ...
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
            logger.info("Acknowledged message: " + message.getId());
        } catch (RedisSystemException e) {
            logger.severe("Error processing message " + message.getId() + ": " + e.getMessage());
        }
    }
}
