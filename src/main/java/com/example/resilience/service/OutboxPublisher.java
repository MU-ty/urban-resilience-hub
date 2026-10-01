package com.example.resilience.service;

import com.example.resilience.config.MessagingConfig;
import com.example.resilience.domain.OutboxEvent;
import com.example.resilience.domain.Enums.OutboxStatus;
import com.example.resilience.repository.OutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisher {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository events;
    private final RabbitTemplate rabbit;
    public OutboxPublisher(OutboxRepository events, RabbitTemplate rabbit) { this.events = events; this.rabbit = rabbit; }

    @Scheduled(fixedDelayString = "${app.outbox.interval:1000}")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch = events.findBatch(OutboxStatus.PENDING, Instant.now(), PageRequest.of(0, 50));
        for (OutboxEvent event : batch) {
            try {
                var correlation = new org.springframework.amqp.rabbit.connection.CorrelationData(event.getId().toString());
                rabbit.convertAndSend(MessagingConfig.EXCHANGE, event.getEventType(), event.getPayload(), message -> {
                    message.getMessageProperties().setMessageId(event.getId().toString());
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                }, correlation);
                var confirm = correlation.getFuture().get(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!confirm.ack() || correlation.getReturned() != null) {
                    throw new IllegalStateException("消息未被确认或无法路由");
                }
                event.published();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); event.failed(); break;
            } catch (Exception e) {
                event.failed(); log.warn("Outbox 发布失败 eventId={}", event.getId(), e);
            }
        }
    }
}
