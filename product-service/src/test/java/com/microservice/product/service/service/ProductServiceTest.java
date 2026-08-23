package com.microservice.product.service.service;

import com.microservice.product.service.api.ProductRequest;
import com.microservice.product.service.api.ProductResponse;
import com.microservice.product.service.model.Product;
import com.microservice.product.service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createsAndReturnsProduct() {
        ProductRequest request = new ProductRequest("Keyboard", "Mechanical keyboard", new BigDecimal("99.99"));
        Product savedProduct = Product.builder()
                .id("product-1")
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isEqualTo("product-1");
        assertThat(response.price()).isEqualByComparingTo("99.99");
        verify(productRepository).save(any(Product.class));
    }
}
