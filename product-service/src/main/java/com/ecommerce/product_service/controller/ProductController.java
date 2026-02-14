package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        logger.info("GET /products - Buscando todos os produtos");
        
        try {
            List<ProductDTO> products = productService.getAllProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao buscar produtos", e);
            return buildErrorResponse("Erro ao buscar produtos", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        logger.info("GET /products/{} - Buscando produto", id);
        
        try {
            ProductDTO product = productService.getProductById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", product);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao buscar produto com ID: {}", id, e);
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategory(@PathVariable String category) {
        logger.info("GET /products/category/{} - Buscando produtos por categoria", category);
        
        try {
            List<ProductDTO> products = productService.getProductsByCategory(category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            response.put("category", category);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao buscar produtos por categoria: {}", category, e);
            return buildErrorResponse("Erro ao buscar produtos por categoria", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableProducts() {
        logger.info("GET /products/available - Buscando produtos disponíveis");
        
        try {
            List<ProductDTO> products = productService.getAvailableProducts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("count", products.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao buscar produtos disponíveis", e);
            return buildErrorResponse("Erro ao buscar produtos disponíveis", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody ProductDTO productDTO) {
        logger.info("POST /products - Criando novo produto: {}", productDTO.getName());
        
        try {
            if (productDTO.getName() == null || productDTO.getName().isBlank()) {
                return buildErrorResponse("Nome do produto é obrigatório", HttpStatus.BAD_REQUEST);
            }
            if (productDTO.getPrice() == null) {
                return buildErrorResponse("Preço do produto é obrigatório", HttpStatus.BAD_REQUEST);
            }
            if (productDTO.getSku() == null || productDTO.getSku().isBlank()) {
                return buildErrorResponse("SKU do produto é obrigatório", HttpStatus.BAD_REQUEST);
            }
            if (productDTO.getQuantity() == null) {
                return buildErrorResponse("Quantidade é obrigatória", HttpStatus.BAD_REQUEST);
            }

            ProductDTO createdProduct = productService.createProduct(productDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produto criado com sucesso");
            response.put("data", createdProduct);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Erro de validação ao criar produto: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Erro ao criar produto", e);
            return buildErrorResponse("Erro ao criar produto", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDTO productDTO) {
        logger.info("PUT /products/{} - Atualizando produto", id);
        
        try {
            ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produto atualizado com sucesso");
            response.put("data", updatedProduct);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao atualizar produto com ID: {}", id, e);
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        logger.info("DELETE /products/{} - Deletando produto", id);
        
        try {
            productService.deleteProduct(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produto deletado com sucesso");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao deletar produto com ID: {}", id, e);
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service funcionando!");
    }

    /**
     * Constrói resposta de erro padrão
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }
}
