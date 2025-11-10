package com.multichunk.demo.configuration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    private final RedisProperties redisProperties;

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    // When the application context is closed, the RedissonClient will be shut down using the shutdown method in RedissonClient.
    //Internally, Spring Boot executes something like this: redissonClient.getClass().getMethod("shutdown").invoke(redissonClient);
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        var config = new Config();
        var address = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());

        config.useSingleServer()
                .setAddress(address);

        return Redisson.create(config);
    }
}
