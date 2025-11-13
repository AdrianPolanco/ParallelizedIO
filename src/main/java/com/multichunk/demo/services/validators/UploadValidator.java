package com.multichunk.demo.services.validators;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class UploadValidator {
    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;
    @Value("${redis-streams.uploads.consumer-group}")
    private String consumerGroup;
    private static final String CONSUMER_NAME = UUID.randomUUID().toString();
    private final Logger logger = Logger.getLogger(UploadValidator.class.getName());
    private final RedisTemplate<String, Object> redisTemplate;

    public UploadValidator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Specialized validation logic for upload messages can be implemented here
 /*   @PostConstruct
    public void init() {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, consumerGroup);
            logger.info("Created consumer group '" + consumerGroup + "' for stream '" + streamKey + "'");
        } catch (Exception e) {
            logger.warning("Consumer group '" + consumerGroup + "' for stream '" + streamKey + "' may already exist: " + e.getMessage());
        }

        startConsumerLoop();
    }

    @Async("uploadExecutor")
    private void startConsumerLoop(){
        consumeMessages();
    }

    private void consumeMessages(){
        while(true){
            try{
                // .opsForStream.read() invokes a blocking read on the stream, internally uses XREADGROUP
                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream()
                        .read(
                                //Specifies the consumer group and consumer name
                                Consumer.from(consumerGroup, CONSUMER_NAME),
                                // Specifies read options: Reads 1 message at a time to control batch size
                                // Waits up to 2 seconds if no messages are available (improves efficiency), returns empty list if timeout
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                // Indicates from where to read messages: last consumed message for this consumer group
                                // Internally uses XREADGROUP ... >
                                StreamOffset.create(streamKey, ReadOffset.lastConsumed()));

                if(messages.isEmpty()){
                    continue;
                }

                for(MapRecord<String, Object, Object> message : messages){
                    processMessage(message);
                    // Acknowledge the message after processing
                    redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
                    logger.info("Acknowledged message ID: " + message.getId());
                }

                var claimed = redisTemplate.opsForStream().
            } catch (Exception e){
                logger.severe("Error while consuming messages: " + e.getMessage());
            }
        }
    }

    private void processMessage(MapRecord<String, Object, Object> message){
        logger.info("Processing message ID: " + message.getId() + " with body: " + message.getValue());
        // Add validation logic here

    }*/
}
