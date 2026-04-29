package com.example.storeapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.Cookie;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
public class CartIntegrationTest extends IntegrationTestBase {

    private Cookie sessionCookie;
    private String userEmail;

    @BeforeEach
    void setUp() throws Exception {
        userEmail = "cart_" + UUID.randomUUID() + "@example.com";
        sessionCookie = registerAndLogin(userEmail, "password123");
    }

    @Test
    void addToCart_shouldReturnOk_whenValidRequest() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(1))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    @WithMockUser(username = "anonymous", roles = {})
    void addToCart_shouldReturn401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 2
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addToCart_shouldReturnError_whenNotEnoughStock() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 9999
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addToCart_shouldReturnError_whenProductAlreadyInCart() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 1
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCart_shouldReturnCartWithSubtotal() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 2
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cart")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].ordinal").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal").exists());
    }

    @Test
    @WithMockUser(username = "anonymous", roles = {})
    void getCart_shouldReturn401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modifyCartItem_shouldUpdateQuantity() throws Exception {
        MvcResult addResult = mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long cartItemId = com.jayway.jsonpath.JsonPath
                .parse(addResult.getResponse().getContentAsString())
                .read("$.id", Long.class);

        mockMvc.perform(put("/api/cart/" + cartItemId)
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "quantity": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void removeCartItem_shouldReturnOk() throws Exception {
        MvcResult addResult = mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long cartItemId = com.jayway.jsonpath.JsonPath
                .parse(addResult.getResponse().getContentAsString())
                .read("$.id", Long.class);

        mockMvc.perform(delete("/api/cart/" + cartItemId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string("Item removed from cart"));
    }

    @Test
    void checkout_shouldReturnOk_whenCartHasItems() throws Exception {
        mockMvc.perform(post("/api/cart")
                        .contentType(APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content("""
                                {
                                    "productId": 1,
                                    "quantity": 1
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/checkout")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Order #")));
    }

    @Test
    void checkout_shouldReturnError_whenCartIsEmpty() throws Exception {
        mockMvc.perform(post("/api/cart/checkout")
                        .cookie(sessionCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }
}