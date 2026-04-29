package com.example.storeapi.service;

import com.example.storeapi.dto.ProductResponse;
import com.example.storeapi.model.Product;
import com.example.storeapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product p1;
    private Product p2;

    @BeforeEach
    void setUp() {
        p1 = new Product(1L, "Nail gun", 5, new BigDecimal("23.95"));
        p2 = new Product(2L, "Hammer", 10, new BigDecimal("9.50"));
    }

    @Test
    void getAllProducts_returnsMappedResponses() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(0).getTitle()).isEqualTo("Nail gun");
        assertThat(responses.get(1).getPrice()).isEqualByComparingTo(new BigDecimal("9.50"));
    }

    @Test
    void getProductById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));

        Product product = productService.getProductById(1L);

        assertThat(product).isNotNull();
        assertThat(product.getTitle()).isEqualTo("Nail gun");
    }

    @Test
    void getProductById_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 99");
    }
}

