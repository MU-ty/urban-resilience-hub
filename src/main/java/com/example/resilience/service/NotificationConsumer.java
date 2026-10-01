package com.example.resilience.service;

import com.example.resilience.config.MessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final StringRedisTemplate redis;
    public NotificationConsumer(StringRedisTemplate redis) { this.redis = redis; }

    @RabbitListener(queues = MessagingConfig.QUEUE)
    public void consume(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        Boolean first = redis.opsForValue().setIfAbsent("message:consumed:" + messageId, "1", Duration.ofDays(7));
        if (!Boolean.TRUE.equals(first)) return; // 消费端幂等，支持至少一次投递
        log.info("模拟发送分配通知: {}", new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
