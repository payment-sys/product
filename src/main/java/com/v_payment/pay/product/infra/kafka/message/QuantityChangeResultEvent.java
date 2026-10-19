package com.v_payment.pay.product.infra.kafka.message;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public record QuantityChangeResultEvent(
        String orderCode,
        Long productId,
        String productName,
        Long unitPrice,
        Integer changeCount,
        QuantityChangeStatus changeStatus,
        Integer productsCount
) {
    public static QuantityChangeResultEvent of(
            String orderCode,
            Long productId,
            String productName,
            Long unitPrice,
            Integer changeCount,
            ChangeStatus changeStatus,
            Integer productsCount
    ) {
        return new QuantityChangeResultEvent(
                orderCode,
                productId,
                productName,
                unitPrice,
                changeCount,
                QuantityChangeStatus.from(changeStatus),
                productsCount
        );
    }
}
