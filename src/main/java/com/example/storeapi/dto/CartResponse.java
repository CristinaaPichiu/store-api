package com.example.storeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Full cart contents with subtotal")
public class CartResponse {

    @Schema(description = "List of items in the cart")
    private List<CartItemResponse> items;

    @Schema(description = "Cart subtotal", example = "71.85")
    private BigDecimal subtotal;
}