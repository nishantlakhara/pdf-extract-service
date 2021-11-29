package com.microservices.arch.async.pdfextractservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${pdf-text-service.thread-pool-executor.corePoolSize}")
    private int corePoolSize;

    @Value("${pdf-text-service.thread-pool-executor.maxPoolSize}")
    private int maxPoolSize;

    @Value("${pdf-text-service.thread-pool-executor.queueCapacity}")
    private int queueCapacity;

    @Value("${pdf-text-service.thread-pool-executor.threadNamePrefix}")
    private String threadNamePrefix;

    @Bean(name="taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }

//    @Bean(name="pdfboxMultiExecutor")
//    public Executor pdfboxMultiExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(50);
//        executor.setMaxPoolSize(50);
//        executor.setQueueCapacity(1000);
//        executor.setThreadNamePrefix("pdfBoxMulti-");
//        executor.initialize();
//        return executor;
//    }
}
