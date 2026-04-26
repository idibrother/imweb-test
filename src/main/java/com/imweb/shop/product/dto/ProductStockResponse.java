package com.imweb.shop.product.dto;

import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;

import java.util.List;

public record ProductStockResponse(
        List<ProductStockItem> products,
        int page,
        int size,
        long totalElements
) {
    public record ProductStockItem(
            Long productId,
            String productName,
            Long price,
            Integer currentStockQuantity,
            ProductCategory category,
            ProductSaleStatus saleStatus
    ) {}
}
