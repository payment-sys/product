package com.v_payment.pay.product.infra.kafka.message;

public record QuantityChangeCommand(
        String orderCode,
        Long productId,
        Integer changeCount,
        Integer productsCount,
        QuantityChangeType type
) {
    public QuantityChangeCommand {
        type = type == null ? QuantityChangeType.DECREASE : type;
    }
}
