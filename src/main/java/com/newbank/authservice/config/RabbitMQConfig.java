package com.newbank.authservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String AUTH_EXCHANGE = "auth-exchange";
    
    // Queues
    public static final String USER_SIGNUP_QUEUE = "user-signup-queue";
    public static final String USER_SIGNUP_DLQ = "user-signup-dlq";
    public static final String LOGIN_AUDIT_QUEUE = "login-audit-queue";
    public static final String LOGIN_AUDIT_DLQ = "login-audit-dlq";

    // Routing Keys
    public static final String USER_SIGNUP_ROUTING_KEY = "auth.user.signup";
    public static final String LOGIN_AUDIT_ROUTING_KEY = "auth.user.login";

    // Main Exchange
    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE, true, false);
    }

    // User Signup Queue
    @Bean
    public Queue userSignupQueue() {
        return QueueBuilder.durable(USER_SIGNUP_QUEUE)
                .withArgument("x-dead-letter-exchange", AUTH_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "auth.dlq.signup")
                .build();
    }

    @Bean
    public Queue userSignupDLQ() {
        return new Queue(USER_SIGNUP_DLQ, true);
    }

    @Bean
    public Binding userSignupBinding(Queue userSignupQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userSignupQueue)
                .to(authExchange)
                .with(USER_SIGNUP_ROUTING_KEY);
    }

    @Bean
    public Binding userSignupDLQBinding(Queue userSignupDLQ, TopicExchange authExchange) {
        return BindingBuilder.bind(userSignupDLQ)
                .to(authExchange)
                .with("auth.dlq.signup");
    }

    // Login Audit Queue
    @Bean
    public Queue loginAuditQueue() {
        return QueueBuilder.durable(LOGIN_AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", AUTH_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "auth.dlq.login")
                .build();
    }

    @Bean
    public Queue loginAuditDLQ() {
        return new Queue(LOGIN_AUDIT_DLQ, true);
    }

    @Bean
    public Binding loginAuditBinding(Queue loginAuditQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(loginAuditQueue)
                .to(authExchange)
                .with(LOGIN_AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding loginAuditDLQBinding(Queue loginAuditDLQ, TopicExchange authExchange) {
        return BindingBuilder.bind(loginAuditDLQ)
                .to(authExchange)
                .with("auth.dlq.login");
    }
}
