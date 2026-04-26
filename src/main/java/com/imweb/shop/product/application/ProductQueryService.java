package com.imweb.shop.product.application;

import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;
import com.imweb.shop.product.infrastructure.ProductRepository;
import com.imweb.shop.product.dto.ProductStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductStockResponse getProductStocks(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage;

        if (category != null && !category.isBlank()) {
            ProductCategory productCategory = ProductCategory.valueOf(category.toUpperCase());
            productPage = productRepository.findByCategoryAndSaleStatusNot(
                    productCategory, ProductSaleStatus.STOPPED, pageable);
        } else {
            productPage = productRepository.findBySaleStatusNot(ProductSaleStatus.STOPPED, pageable);
        }

        List<ProductStockResponse.ProductStockItem> items = productPage.getContent().stream()
                .map(p -> new ProductStockResponse.ProductStockItem(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getStockQuantity(),
                        p.getCategory(),
                        p.getSaleStatus()
                ))
                .collect(Collectors.toList());

        return new ProductStockResponse(
                items,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements()
        );
    }
}
