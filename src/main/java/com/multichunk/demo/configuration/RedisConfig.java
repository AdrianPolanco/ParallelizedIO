package com.multichunk.demo.configuration;

import com.multichunk.demo.components.UploadStreamListener;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

@Configuration
public class RedisConfig {
    @Value("${redis-streams.uploads.consumer-group}")
    private String consumerGroup;
    @Value("${redis-streams.uploads.stream-key}")
    private String streamKey;
    private final RedisProperties redisProperties;
    private final Logger logger = Logger.getLogger(RedisConfig.class.getName());

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    // When the application context is closed, the RedissonClient will be shut down using the shutdown method in RedissonClient.
    //Internally, Spring Boot executes something like this: redissonClient.getClass().getMethod("shutdown").invoke(redissonClient);
   /* @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        var config = new Config();
        var address = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());

        config.useSingleServer()
                .setAddress(address);

        return Redisson.create(config);
    }*/

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }

    @PostConstruct
    public void initRedisGroups() {
        try (var connection = redisConnectionFactory().getConnection()) {
            connection.streamCommands().xGroupCreate(
                    "minio_uploads".getBytes(),
                    "minio_uploads_group",
                    ReadOffset.latest(),
                    true
            );
            logger.info("Redis stream group created successfully");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                logger.info("Redis group already exists");
            } else {
                logger.severe("Error creating Redis group: " + e.getMessage());
            }
        }
    }



    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> redisStreamContainer(
            RedisConnectionFactory connectionFactory, UploadStreamListener uploadStreamListener,
            @Qualifier("redisJsonTemplate") RedisTemplate<String, Object> redisTemplate) throws UnknownHostException {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .batchSize(1)
                        .pollTimeout(Duration.ofSeconds(2))
                        //.executor(executor)
                        .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    @Bean
    public RedisTemplate<String, Object> redisJsonTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        var jsonSerializer = new GenericJackson2JsonRedisSerializer();
        var stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Set the default serializer to JSON for any other serialization needs not explicitly covered above.
        template.setDefaultSerializer(jsonSerializer);

        // Finalize the template setup and initialize it, validating everything is set correctly.
        template.afterPropertiesSet();

        return template;
    }
}
