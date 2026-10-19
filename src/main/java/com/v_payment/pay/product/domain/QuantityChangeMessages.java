package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.infra.kafka.message.QuantityChangeCommand;
import com.v_payment.pay.product.domain.entity.QuantityChangeLedger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class QuantityChangeMessages {
    private final Map<Long, List<QuantityChangeCommand>> messages;

    private QuantityChangeMessages(List<QuantityChangeCommand> messages) {
        this.messages = messages.stream().collect(Collectors.groupingBy(QuantityChangeCommand::productId));
    }

    public List<Long> getAllProductIds() {
        return new ArrayList<>(messages.keySet());
    }

    public List<QuantityChangeCommand> getAllMessages() {
        return messages.values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    public void deleteNotIdempotencyMessages(Collection<QuantityChangeLedger> ledgers) {
        if (ledgers == null || ledgers.isEmpty()) return;
        for (QuantityChangeLedger ledger : ledgers) {
            List<QuantityChangeCommand> productMessages = messages.get(ledger.getProductId());
            if (productMessages == null) continue;
            productMessages.removeIf(message -> isSame(ledger, message));
            if (productMessages.isEmpty()) messages.remove(ledger.getProductId());
        }
    }

    private boolean isSame(QuantityChangeLedger ledger, QuantityChangeCommand message) {
        return Objects.equals(message.orderCode(), ledger.getOrderCode())
                && message.type() == ledger.getChangeType();
    }

    public static QuantityChangeMessages from(List<QuantityChangeCommand> messages) {
        return new QuantityChangeMessages(messages);
    }
}
