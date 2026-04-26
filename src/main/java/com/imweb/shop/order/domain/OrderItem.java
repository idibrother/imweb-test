package com.imweb.shop.order.domain;

import com.imweb.shop.product.domain.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long totalPrice;

    public static OrderItem of(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.productId = product.getId();
        item.productName = product.getName();
        item.unitPrice = product.getPrice();
        item.quantity = quantity;
        item.totalPrice = product.getPrice() * quantity;
        return item;
    }
}
