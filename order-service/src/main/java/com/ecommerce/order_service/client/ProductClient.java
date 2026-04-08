package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.ProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    ProductResponse getProductById(@PathVariable("id") Long id);

    default ProductResponse getProductFallback(Long id, Exception ex) {
        ProductResponse fallback = new ProductResponse();
        fallback.setId(id);
        fallback.setName("Product Service Unavailable");
        fallback.setPrice(BigDecimal.ZERO);
        fallback.setStock(0);
        return fallback;
    }
}
