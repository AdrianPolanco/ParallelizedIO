package com.multichunk.demo.components;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class RedisStreamsInitializer {
    private final StreamMessageListenerContainer<String, ObjectRecord<String, Object>> container;
    private final UploadStreamListener uploadStreamListener;

    @Value("${redis-streams.uploads.consumer-group}")
    private String consumerGroup;
    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;

    public RedisStreamsInitializer(
            StreamMessageListenerContainer<String, ObjectRecord<String, Object>> container,
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
