package com.project.NewBank.Service.AsyncServices;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Async("NotificationExecutor")
    public void sendNotification(String message) {
        try {
            Thread.sleep(2000); 
            System.out.println("Notification sent: " + message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Notification sending interrupted");
        }
    }
}
