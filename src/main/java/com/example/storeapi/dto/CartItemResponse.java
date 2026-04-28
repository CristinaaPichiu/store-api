package com.example.storeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "A single item in the cart")
public class CartItemResponse {

    @Schema(description = "Ordinal position in cart", example = "1")
    private Integer ordinal;

    @Schema(description = "Cart item ID", example = "1")
    private Long cartItemId;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Product title", example = "Nail gun")
    private String productTitle;

    @Schema(description = "Quantity in cart", example = "2")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "23.95")
    private BigDecimal unitPrice;

    @Schema(description = "Total price for this item", example = "47.90")
    private BigDecimal totalPrice;
}