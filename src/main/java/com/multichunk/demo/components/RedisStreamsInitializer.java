package com.multichunk.demo.components;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class RedisStreamsInitializer {
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final UploadStreamListener uploadStreamListener;

    @Value("${redis-streams.uploads.consumer-group}")
    private String consumerGroup;
    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;

    public RedisStreamsInitializer(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            UploadStreamListener uploadStreamListener) {
        this.container = container;
        this.uploadStreamListener = uploadStreamListener;
    }

    @PostConstruct
    public void init() throws UnknownHostException {
        // Set offset to last consumed message for the consumer group
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        container.receive(
                Consumer.from(consumerGroup, InetAddress.getLocalHost().getHostName()),
                streamOffset,
                uploadStreamListener);

        container.start();
    }
}
