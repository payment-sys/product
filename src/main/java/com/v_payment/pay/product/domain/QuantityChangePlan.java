package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.infra.kafka.message.QuantityChangeCommand;

import java.util.*;
import java.util.stream.Collectors;

public class QuantityChangePlan {
    private final List<QuantityChangeCommand> success = new ArrayList<>();
    private final List<QuantityChangeCommand> fail = new ArrayList<>();
    private final Map<Long, List<QuantityChangeCommand>> messageMap = new HashMap<>();

    private QuantityChangePlan(List<QuantityChangeCommand> messages) {
        List<QuantityChangeCommand> quantityChangeMessages = validateOriginMessages(messages);
        for (QuantityChangeCommand msg : quantityChangeMessages) {
            messageMap.computeIfAbsent(msg.productId(), k -> new ArrayList<>()).add(msg);
        }
    }

    public Map<Long, Integer> getSuccessChangeQuantities() {
        return success.stream().collect(Collectors.toMap(
                        QuantityChangeCommand::productId,
                        QuantityChangeCommand::changeCount, Integer::sum
                ));
    }

    public static QuantityChangePlan calculate(
            Map<Long, ProductSnapShot> productSnapShots,
            QuantityChangeMessages messages
    ) {
        QuantityChangePlan plan = new QuantityChangePlan(messages.getAllMessages());
        for (Map.Entry<Long, List<QuantityChangeCommand>> entry : plan.messageMap.entrySet()) {
            Long productId = entry.getKey();
            List<QuantityChangeCommand> productMessages = entry.getValue();
            ProductSnapShot productSnapShot = productSnapShots.get(productId);

            if (productSnapShot == null) {
                plan.fail.addAll(productMessages);
                continue;
            }

            for (QuantityChangeCommand message : productMessages) {
                if (!productSnapShot.canChange(message.changeCount())) {
                    plan.fail.add(message);
                    continue;
                }
                plan.success.add(message);
                productSnapShot.change(message.changeCount());
            }
        }

        return plan;
    }

    private List<QuantityChangeCommand> validateOriginMessages(List<QuantityChangeCommand> messages) {
        if(messages == null) throw new IllegalStateException("messages는 없을 수 없습니다.");
        return messages;
    }

    public List<QuantityChangeCommand> getSuccess() {
        return new ArrayList<>(success);
    }

    public List<QuantityChangeCommand> getFail() {
        return new ArrayList<>(fail);
    }
}
