package com.imweb.shop.product;

import com.imweb.shop.product.application.ProductQueryService;
import com.imweb.shop.product.domain.Product;
import com.imweb.shop.product.domain.ProductCategory;
import com.imweb.shop.product.domain.ProductSaleStatus;
import com.imweb.shop.product.infrastructure.ProductRepository;
import com.imweb.shop.product.dto.ProductStockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductQueryService productQueryService;

    private Product onSaleProduct;
    private Product soldOutProduct;
    private Product stoppedProduct;

    @BeforeEach
    void setUp() {
        onSaleProduct = new Product("청바지", 59000L, 100, ProductCategory.FASHION, ProductSaleStatus.ON_SALE);
        soldOutProduct = new Product("크림", 35000L, 0, ProductCategory.BEAUTY, ProductSaleStatus.SOLD_OUT);
        stoppedProduct = new Product("과자", 5000L, 50, ProductCategory.FOOD, ProductSaleStatus.STOPPED);
    }

    @Test
    @DisplayName("카테고리 없이 조회하면 STOPPED 제외 전체 상품을 반환한다")
    void getProductStocks_noCategory() {
        // given
        List<Product> products = List.of(onSaleProduct, soldOutProduct);
        when(productRepository.findBySaleStatusNot(eq(ProductSaleStatus.STOPPED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(products, PageRequest.of(0, 20), 2));

        // when
        ProductStockResponse response = productQueryService.getProductStocks(null, 0, 20);

        // then
        assertThat(response.products()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        verify(productRepository).findBySaleStatusNot(eq(ProductSaleStatus.STOPPED), any(Pageable.class));
        verify(productRepository, never()).findByCategoryAndSaleStatusNot(any(), any(), any());
    }

    @Test
    @DisplayName("카테고리로 필터링하면 해당 카테고리 상품만 반환한다")
    void getProductStocks_withCategory() {
        // given
        List<Product> fashionProducts = List.of(onSaleProduct);
        when(productRepository.findByCategoryAndSaleStatusNot(
                eq(ProductCategory.FASHION), eq(ProductSaleStatus.STOPPED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(fashionProducts, PageRequest.of(0, 20), 1));

        // when
        ProductStockResponse response = productQueryService.getProductStocks("FASHION", 0, 20);

        // then
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).category()).isEqualTo(ProductCategory.FASHION);
        verify(productRepository).findByCategoryAndSaleStatusNot(
                eq(ProductCategory.FASHION), eq(ProductSaleStatus.STOPPED), any(Pageable.class));
    }

    @Test
    @DisplayName("STOPPED 상품은 반환되지 않는다")
    void getProductStocks_excludesStopped() {
        // given
        when(productRepository.findBySaleStatusNot(eq(ProductSaleStatus.STOPPED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(onSaleProduct), PageRequest.of(0, 20), 1));

        // when
        ProductStockResponse response = productQueryService.getProductStocks(null, 0, 20);

        // then
        assertThat(response.products()).noneMatch(p -> p.saleStatus() == ProductSaleStatus.STOPPED);
    }

    @Test
    @DisplayName("페이지 정보가 올바르게 반환된다")
    void getProductStocks_paginationCorrect() {
        // given
        when(productRepository.findBySaleStatusNot(eq(ProductSaleStatus.STOPPED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(onSaleProduct), PageRequest.of(1, 5), 10));

        // when
        ProductStockResponse response = productQueryService.getProductStocks(null, 1, 5);

        // then
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(10);
    }

    @Test
    @DisplayName("빈 카테고리 문자열은 전체 조회로 처리된다")
    void getProductStocks_blankCategoryTreatedAsNull() {
        // given
        when(productRepository.findBySaleStatusNot(eq(ProductSaleStatus.STOPPED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // when
        ProductStockResponse response = productQueryService.getProductStocks("  ", 0, 20);

        // then
        verify(productRepository).findBySaleStatusNot(eq(ProductSaleStatus.STOPPED), any(Pageable.class));
    }
}
