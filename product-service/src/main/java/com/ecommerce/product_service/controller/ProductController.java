package com.ecommerce.product_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    @GetMapping("/products")
    public String listarProdutos() {
        return "Product Service funcionando!";
    }
}
