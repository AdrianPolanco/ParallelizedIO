package com.multichunk.demo.components;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
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
            Object value = message.getValue();
            String json;

            if (value instanceof byte[] bytes) {
                json = new String(bytes);
            } else {
                json = value.toString();
            }

            var mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(json, Map.class);

            logger.info("Processing message ID: " + message.getId() + " Body: " + map);


            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
            logger.info("Acknowledged message: " + message.getId());
        } catch (RedisSystemException e) {
            logger.severe("Error processing message " + message.getId() + ": " + e.getMessage());
        } catch (StreamReadException e) {
            logger.severe("Error reading stream for message " + message.getId() + ": " + e.getMessage());
        } catch (DatabindException e) {
            logger.severe("Error binding data for message " + message.getId() + ": " + e.getMessage());
        } catch (IOException e) {
            logger.severe("IO Error processing message " + message.getId() + ": " + e.getMessage());
        }
    }
}
