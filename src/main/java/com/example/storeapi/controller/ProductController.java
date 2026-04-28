package com.example.storeapi.controller;

import com.example.storeapi.dto.ProductResponse;
import com.example.storeapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Browse available products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all products in the store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of products returned successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
}