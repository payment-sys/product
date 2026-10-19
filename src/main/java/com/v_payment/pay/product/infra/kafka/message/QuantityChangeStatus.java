package com.v_payment.pay.product.infra.kafka.message;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public enum QuantityChangeStatus {
    SUCCESS, FAILED;

    public static QuantityChangeStatus from(ChangeStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("change status is required.");
        }
        return QuantityChangeStatus.valueOf(status.name());
    }
}
