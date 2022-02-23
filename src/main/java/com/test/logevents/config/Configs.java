package com.test.logevents.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class Configs {

    @Bean
    public TaskExecutor logEventsTaskExecutor(@Value("${pool.size.min:0}") final int minPoolSize,
                                              @Value("${pool.size.max}") final int maxPoolSize) {
        final int corePoolSize = minPoolSize <= 0 ? Runtime.getRuntime().availableProcessors() : minPoolSize;
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(Math.max(maxPoolSize, corePoolSize));
        taskExecutor.setThreadNamePrefix("events_thread_");
        return taskExecutor;
    }

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
