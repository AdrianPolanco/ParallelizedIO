package com.multichunk.demo.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class UploadStreamListener implements StreamListener<String, MapRecord<String, String, String>> {
    private static final Logger logger = Logger.getLogger(UploadStreamListener.class.getName());
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis-streams.uploads.consumer-group}")
    private String consumerGroup;

    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;

    public UploadStreamListener(@Qualifier("redisJsonTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
            try {
                Object payload = message.getValue().get("payload");
                logger.info("Processing message ID: " + message.getId() + " Body: " + payload);
                // Acknowledge the message after processing
                redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
                logger.info("Acknowledged message: " + message.getId());
            } catch (RedisSystemException e) {
                logger.severe("Error processing message " + message.getId() + ": " + e.getMessage());
            }
        }
    }
