package com.example.storeapi.service;

import com.example.storeapi.dto.AddToCartRequest;
import com.example.storeapi.dto.CartItemResponse;
import com.example.storeapi.dto.CartResponse;
import com.example.storeapi.model.*;
import com.example.storeapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public CartItem addToCart(String sessionId, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getAvailable() < request.getQuantity()) {
            throw new RuntimeException("Not enough stock. Available: " + product.getAvailable());
        }

        if (cartItemRepository.findBySessionIdAndProductId(sessionId, request.getProductId()).isPresent()) {
            throw new RuntimeException("Product already in cart. Use modify to change quantity.");
        }

        CartItem cartItem = new CartItem();
        cartItem.setSessionId(sessionId);
        cartItem.setProduct(product);
        cartItem.setQuantity(request.getQuantity());

        return cartItemRepository.save(cartItem);
    }

    public CartResponse getCart(String sessionId) {
        List<CartItem> items = cartItemRepository.findBySessionId(sessionId);

        AtomicInteger ordinal = new AtomicInteger(1);

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    BigDecimal totalPrice = item.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(
                            ordinal.getAndIncrement(),
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getTitle(),
                            item.getQuantity(),
                            item.getProduct().getPrice(),
                            totalPrice
                    );
                })
                .collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(itemResponses, subtotal);
    }

    public CartItem modifyCartItem(String sessionId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Cart item does not belong to this session");
        }

        Product product = cartItem.getProduct();
        if (product.getAvailable() < quantity) {
            throw new RuntimeException("Not enough stock. Available: " + product.getAvailable());
        }

        cartItem.setQuantity(quantity);
        return cartItemRepository.save(cartItem);
    }

    public void removeCartItem(String sessionId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Cart item does not belong to this session");
        }

        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public Order checkout(String sessionId, Long userId) {
        List<CartItem> cartItems = cartItemRepository.findBySessionId(sessionId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getAvailable() < cartItem.getQuantity()) {
                throw new RuntimeException("Not enough stock for: " + product.getTitle()
                        + ". Available: " + product.getAvailable());
            }

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotal(total);
        order.setStatus("CONFIRMED");
        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItemRepository.save(orderItem);

            product.setAvailable(product.getAvailable() - cartItem.getQuantity());
            productRepository.save(product);
        }

        cartItemRepository.deleteBySessionId(sessionId);

        return savedOrder;
    }
}