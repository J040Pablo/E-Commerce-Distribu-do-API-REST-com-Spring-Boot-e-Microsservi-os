package com.ecommerce.order_service.client;

import com.ecommerce.order_service.dto.CustomerResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    @GetMapping("/api/customers/{id}")
    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customerService")
    CustomerResponse getCustomerById(@PathVariable("id") Long id);

    default CustomerResponse getCustomerFallback(Long id, Exception ex) {
        CustomerResponse fallback = new CustomerResponse();
        fallback.setId(id);
        fallback.setName("Customer Service Unavailable");
        fallback.setEmail("unavailable@service.com");
        return fallback;
    }
}
