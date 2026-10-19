package com.v_payment.pay.product.infra.kafka;

import com.v_payment.pay.global.meter.KafkaMetrics;
import com.v_payment.pay.product.config.QuantityChangeResultProducerProperties;
import com.v_payment.pay.product.infra.kafka.message.QuantityChangeResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class QuantityChangeResultProducer {
    private final QuantityChangeResultProducerProperties quantityChangeResultProducerProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaMetrics kafkaMetrics;

    public CompletableFuture<?> send(QuantityChangeResultEvent quantityChangeResultEvent) {
        String topic = quantityChangeResultProducerProperties.topic();
        String key = quantityChangeResultEvent.orderCode();
        String message = objectMapper.writeValueAsString(quantityChangeResultEvent);
        return kafkaMetrics.recordProducerSend(topic, kafkaTemplate.send(topic, key, message));
    }
}
