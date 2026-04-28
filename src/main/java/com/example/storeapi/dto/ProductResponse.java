package com.example.storeapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "Product information returned by the API")
public class ProductResponse {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @Schema(description = "Product title", example = "Nail gun")
    private String title;

    @Schema(description = "Available quantity in stock", example = "8")
    private Integer available;

    @Schema(description = "Product price", example = "23.95")
    private BigDecimal price;
}