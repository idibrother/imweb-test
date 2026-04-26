package com.imweb.shop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PaymentRequest {

    @NotNull
    private Long userId;

    @NotEmpty
    @Valid
    private List<PaymentItemRequest> items;

    private List<Long> userCouponIds;

    public record PaymentItemRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity
    ) {}
}
