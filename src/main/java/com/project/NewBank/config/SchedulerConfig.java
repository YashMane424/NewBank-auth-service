package com.project.NewBank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean (name="taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);                           // 3 threads: daily, weekly, cleanup
        scheduler.setThreadNamePrefix("BankScheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);    //Ensure tasks complete on shutdown
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}