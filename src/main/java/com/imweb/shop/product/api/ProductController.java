package com.imweb.shop.product.api;

import com.imweb.shop.product.application.ProductQueryService;
import com.imweb.shop.product.dto.ProductStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductQueryService productQueryService;

    @GetMapping("/stocks")
    public ResponseEntity<ProductStockResponse> getProductStocks(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productQueryService.getProductStocks(category, page, size));
    }
}
