package com.imweb.shop.product.domain;

import com.imweb.shop.global.exception.ErrorCode;
import com.imweb.shop.global.exception.PaymentDomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductSaleStatus saleStatus;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Product(String name, Long price, Integer stockQuantity, ProductCategory category, ProductSaleStatus saleStatus) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.saleStatus = saleStatus;
    }

    public void validatePurchasable(int quantity) {
        if (this.saleStatus != ProductSaleStatus.ON_SALE) {
            throw new PaymentDomainException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        if (this.stockQuantity < quantity) {
            throw new PaymentDomainException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    public void decreaseStock(int quantity) {
        validatePurchasable(quantity);
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }
}
