package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        logger.info("GET /products - Buscando todos os produtos");

        List<ProductDTO> products = productService.getAllProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        logger.info("GET /products/{} - Buscando produto", id);

        ProductDTO product = productService.getProductById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", product);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategory(@PathVariable String category) {
        logger.info("GET /products/category/{} - Buscando produtos por categoria", category);

        List<ProductDTO> products = productService.getProductsByCategory(category);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);
        response.put("count", products.size());
        response.put("category", category);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableProducts() {
        logger.info("GET /products/available - Buscando produtos disponíveis");

        List<ProductDTO> products = productService.getAvailableProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        logger.info("POST /products - Criando novo produto: {}", productDTO.getName());

        ProductDTO createdProduct = productService.createProduct(productDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Produto criado com sucesso");
        response.put("data", createdProduct);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        logger.info("PUT /products/{} - Atualizando produto", id);

        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Produto atualizado com sucesso");
        response.put("data", updatedProduct);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        logger.info("DELETE /products/{} - Deletando produto", id);

        productService.deleteProduct(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Produto deletado com sucesso");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service funcionando!");
    }

}
