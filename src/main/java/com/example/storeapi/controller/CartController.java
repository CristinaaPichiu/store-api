package com.example.storeapi.controller;

import com.example.storeapi.dto.AddToCartRequest;
import com.example.storeapi.dto.CartResponse;
import com.example.storeapi.dto.ModifyCartRequest;
import com.example.storeapi.model.Order;
import com.example.storeapi.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Add a product to the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Not enough stock or product already in cart",
                    content = @Content(schema = @Schema(example = "Not enough stock. Available: 3"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddToCartRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
            }
            return ResponseEntity.ok(cartService.addToCart(session.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Get cart contents with subtotal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<?> getCart(HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
            }
            return ResponseEntity.ok(cartService.getCart(session.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Modify quantity of a cart item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart item modified successfully"),
            @ApiResponse(responseCode = "400", description = "Not enough stock or item not found",
                    content = @Content(schema = @Schema(example = "Not enough stock. Available: 3"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/{cartItemId}")
    public ResponseEntity<?> modifyCartItem(@PathVariable Long cartItemId,
                                            @Valid @RequestBody ModifyCartRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
            }
            return ResponseEntity.ok(cartService.modifyCartItem(session.getId(), cartItemId, request.getQuantity()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Remove an item from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "400", description = "Item not found",
                    content = @Content(schema = @Schema(example = "Cart item not found"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId,
                                            HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
            }
            cartService.removeCartItem(session.getId(), cartItemId);
            return ResponseEntity.ok("Item removed from cart");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Checkout and place order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Cart is empty or not enough stock",
                    content = @Content(schema = @Schema(example = "Not enough stock for: Nail gun. Available: 2"))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(HttpServletRequest httpRequest) {
        try {
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
            }
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login first");
            }
            Order order = cartService.checkout(session.getId(), userId);
            return ResponseEntity.ok("Order #" + order.getId() + " placed successfully. Total: $" + order.getTotal());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}