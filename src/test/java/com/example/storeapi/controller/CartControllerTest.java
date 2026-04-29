package com.example.storeapi.controller;

import com.example.storeapi.dto.AddToCartRequest;
import com.example.storeapi.dto.CartItemResponse;
import com.example.storeapi.dto.CartResponse;
import com.example.storeapi.dto.ModifyCartRequest;
import com.example.storeapi.exception.GlobalExceptionHandler;
import com.example.storeapi.model.CartItem;
import com.example.storeapi.model.Order;
import com.example.storeapi.model.Product;
import com.example.storeapi.model.User;
import com.example.storeapi.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private static final Long TEST_USER_ID = 7L;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Product product;
    private CartItem cartItem;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        product = new Product(1L, "Nail gun", 5, new BigDecimal("23.95"));
        cartItem = new CartItem(10L, "sess-1", product, 2);

        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("secret");
    }

    @Test
    void addToCart_success_returns200() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenReturn(cartItem);

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(createLoggedInSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(cartService).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    void addToCart_noSession_returns401() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));

        verify(cartService, never()).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    void addToCart_productAlreadyInCart_returns400() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenThrow(new RuntimeException("Product already in cart. Use modify to change quantity."));

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product already in cart. Use modify to change quantity."));
    }

    @Test
    void addToCart_insufficientStock_returns400() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(10);

        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenThrow(new RuntimeException("Not enough stock. Available: 5"));

        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough stock. Available: 5"));
    }

    @Test
    void getCart_success_returns200() throws Exception {
        CartItemResponse item = new CartItemResponse(
                1,
                10L,
                1L,
                "Nail gun",
                2,
                new BigDecimal("23.95"),
                new BigDecimal("47.90")
        );

        CartResponse response = new CartResponse(List.of(item), new BigDecimal("47.90"));

        when(cartService.getCart(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/cart")
                        .session(createLoggedInSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].ordinal").value(1))
                .andExpect(jsonPath("$.items[0].cartItemId").value(10))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productTitle").value("Nail gun"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(47.90));
    }

    @Test
    void getCart_noSession_returns401() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));

        verify(cartService, never()).getCart(anyString());
    }

    @Test
    void modifyCartItem_success_returns200() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setQuantity(5);

        CartItem modified = new CartItem(10L, "sess-1", product, 5);

        when(cartService.modifyCartItem(anyString(), eq(10L), eq(5)))
                .thenReturn(modified);

        mockMvc.perform(put("/api/cart/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(createLoggedInSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void modifyCartItem_noSession_returns401() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setQuantity(5);

        mockMvc.perform(put("/api/cart/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));

        verify(cartService, never()).modifyCartItem(anyString(), any(), any());
    }

    @Test
    void modifyCartItem_itemNotFound_returns400() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setQuantity(5);

        when(cartService.modifyCartItem(anyString(), eq(999L), eq(5)))
                .thenThrow(new RuntimeException("Cart item not found"));

        mockMvc.perform(put("/api/cart/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart item not found"));
    }

    @Test
    void modifyCartItem_insufficientStock_returns400() throws Exception {
        ModifyCartRequest request = new ModifyCartRequest();
        request.setQuantity(100);

        when(cartService.modifyCartItem(anyString(), eq(10L), eq(100)))
                .thenThrow(new RuntimeException("Not enough stock. Available: 5"));

        mockMvc.perform(put("/api/cart/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough stock. Available: 5"));
    }

    @Test
    void removeCartItem_success_returns200() throws Exception {
        doNothing().when(cartService).removeCartItem(anyString(), eq(10L));

        mockMvc.perform(delete("/api/cart/10")
                        .session(createLoggedInSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("Item removed from cart"));
    }

    @Test
    void removeCartItem_noSession_returns401() throws Exception {
        mockMvc.perform(delete("/api/cart/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));

        verify(cartService, never()).removeCartItem(anyString(), any());
    }

    @Test
    void removeCartItem_itemNotFound_returns400() throws Exception {
        doThrow(new RuntimeException("Cart item not found"))
                .when(cartService).removeCartItem(anyString(), eq(999L));

        mockMvc.perform(delete("/api/cart/999")
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart item not found"));
    }

    @Test
    void removeCartItem_wrongSession_returns400() throws Exception {
        doThrow(new RuntimeException("Cart item does not belong to this session"))
                .when(cartService).removeCartItem(anyString(), eq(10L));

        mockMvc.perform(delete("/api/cart/10")
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart item does not belong to this session"));
    }

    @Test
    void checkout_success_returns200() throws Exception {
        Order order = new Order();
        order.setId(55L);
        order.setUser(testUser);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotal(new BigDecimal("47.90"));
        order.setStatus("CONFIRMED");

        when(cartService.checkout(anyString(), eq(TEST_USER_ID)))
                .thenReturn(order);

        mockMvc.perform(post("/api/cart/checkout")
                        .session(createLoggedInSession()))
                .andExpect(status().isOk())
                .andExpect(content().string("Order #55 placed successfully. Total: $47.90"));
    }

    @Test
    void checkout_noSession_returns401() throws Exception {
        mockMvc.perform(post("/api/cart/checkout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));

        verify(cartService, never()).checkout(anyString(), any());
    }

    @Test
    void checkout_noUserId_returns401() throws Exception {
        mockMvc.perform(post("/api/cart/checkout")
                        .session(createSessionWithoutUserId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));

        verify(cartService, never()).checkout(anyString(), any());
    }

    @Test
    void checkout_emptyCart_returns400() throws Exception {
        when(cartService.checkout(anyString(), eq(TEST_USER_ID)))
                .thenThrow(new RuntimeException("Cart is empty"));

        mockMvc.perform(post("/api/cart/checkout")
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }

    @Test
    void checkout_insufficientStock_returns400() throws Exception {
        when(cartService.checkout(anyString(), eq(TEST_USER_ID)))
                .thenThrow(new RuntimeException("Not enough stock for: Nail gun. Available: 2"));

        mockMvc.perform(post("/api/cart/checkout")
                        .session(createLoggedInSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough stock for: Nail gun. Available: 2"));
    }

    private MockHttpSession createLoggedInSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", TEST_USER_ID);
        return session;
    }

    private MockHttpSession createSessionWithoutUserId() {
        return new MockHttpSession();
    }
}