package com.example.storeapi.service;

import com.example.storeapi.dto.AddToCartRequest;
import com.example.storeapi.dto.CartResponse;
import com.example.storeapi.model.*;
import com.example.storeapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private Product product;
    private CartItem cartItem;
    private User user;

    @BeforeEach
    void setUp() {
        product = new Product(1L, "Nail gun", 5, new BigDecimal("23.95"));
        cartItem = new CartItem(10L, "sess-1", product, 2);
        user = new User();
        user.setId(7L);
        user.setEmail("test@example.com");
        user.setPasswordHash("secret");
    }

    @Test
    void addToCart_success() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findBySessionIdAndProductId("sess-1", 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> {
            CartItem c = i.getArgument(0);
            c.setId(100L);
            return c;
        });

        CartItem saved = cartService.addToCart("sess-1", request);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getSessionId()).isEqualTo("sess-1");
        assertThat(saved.getProduct().getId()).isEqualTo(1L);
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_whenAlreadyInCart_throws() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findBySessionIdAndProductId("sess-1", 1L)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> cartService.addToCart("sess-1", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product already in cart");
    }

    @Test
    void addToCart_productNotFound_throws() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(999L);
        request.setQuantity(1);

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart("sess-1", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void addToCart_insufficientStock_throws() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addToCart("sess-1", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough stock. Available: 5");
    }

    @Test
    void getCart_calculatesSubtotalAndOrdinal() {
        CartItem item2 = new CartItem(11L, "sess-1", new Product(2L, "Hammer", 3, new BigDecimal("9.50")), 1);
        when(cartItemRepository.findBySessionId("sess-1")).thenReturn(List.of(cartItem, item2));

        CartResponse response = cartService.getCart("sess-1");

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getOrdinal()).isEqualTo(1);
        assertThat(response.getItems().get(1).getOrdinal()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("57.40"));
    }

    @Test
    void modifyCartItem_success() {
        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

        CartItem modified = cartService.modifyCartItem("sess-1", 10L, 3);

        assertThat(modified.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void modifyCartItem_insufficientStock_throws() {
        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(cartItem));

        assertThatThrownBy(() -> cartService.modifyCartItem("sess-1", 10L, 20))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    void modifyCartItem_notFound_throws() {
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.modifyCartItem("sess-1", 999L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void removeCartItem_success() {
        when(cartItemRepository.findById(10L)).thenReturn(Optional.of(cartItem));

        cartService.removeCartItem("sess-1", 10L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void removeCartItem_notFound_throws() {
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeCartItem("sess-1", 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void removeCartItem_wrongSession_throws() {
        CartItem other = new CartItem(20L, "other-sess", product, 1);
        when(cartItemRepository.findById(20L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> cartService.removeCartItem("sess-1", 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to this session");
    }

    @Test
    void checkout_emptyCart_throws() {
        when(cartItemRepository.findBySessionId("sess-1")).thenReturn(List.of());

        assertThatThrownBy(() -> cartService.checkout("sess-1", 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void checkout_happyPath_createsOrderAndDeductsStockAndClearsCart() {
        CartItem ci = new CartItem(10L, "sess-1", product, 2);
        when(cartItemRepository.findBySessionId("sess-1")).thenReturn(List.of(ci));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        Order savedOrder = new Order();
        savedOrder.setId(55L);
        savedOrder.setUser(user);
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setTotal(new BigDecimal("47.90"));
        savedOrder.setStatus("CONFIRMED");

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        Order result = cartService.checkout("sess-1", 7L);

        assertThat(result.getId()).isEqualTo(55L);
        verify(productRepository, atLeastOnce()).save(argThat(p -> p.getAvailable() == 3));
        verify(cartItemRepository, times(1)).deleteBySessionId("sess-1");
    }

    @Test
    void checkout_insufficientStockForItem_throws() {
        Product lowStockProduct = new Product(1L, "Nail gun", 1, new BigDecimal("23.95"));
        CartItem ci = new CartItem(10L, "sess-1", lowStockProduct, 2);
        when(cartItemRepository.findBySessionId("sess-1")).thenReturn(List.of(ci));
        when(productRepository.findById(1L)).thenReturn(Optional.of(lowStockProduct));

        assertThatThrownBy(() -> cartService.checkout("sess-1", 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough stock for");
    }

    @Test
    void checkout_userNotFound_throws() {
        CartItem ci = new CartItem(10L, "sess-1", product, 2);
        when(cartItemRepository.findBySessionId("sess-1")).thenReturn(List.of(ci));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.checkout("sess-1", 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
