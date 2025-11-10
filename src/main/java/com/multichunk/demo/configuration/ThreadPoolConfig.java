package com.multichunk.demo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean(name = "downloadExecutor")
    public Executor downloadExecutor() {
        /*ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("download-worker-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;*/

        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "uploadExecutor")
    public Executor uploadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "scheduleExecutor")
    public ThreadPoolTaskExecutor scheduleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setThreadNamePrefix("progress-scheduler-");
        executor.initialize();
        return executor;
    }
}
