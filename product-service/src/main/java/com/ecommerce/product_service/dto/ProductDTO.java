package com.ecommerce.product_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) para Product
 * Usado para serialização/desserialização em requisições HTTP
 */
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Nome do produto é obrigatório")
    private String name;

    @NotBlank(message = "Descrição do produto é obrigatória")
    private String description;

    @NotNull(message = "Preço do produto é obrigatório")
    @Positive(message = "Preço do produto deve ser maior que zero")
    private BigDecimal price;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantity;

    @NotBlank(message = "SKU do produto é obrigatório")
    private String sku;

    @NotBlank(message = "Categoria do produto é obrigatória")
    private String category;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ProductDTO() {
    }

    public ProductDTO(Long id, String name, String description, BigDecimal price, Integer quantity, 
                      String sku, String category, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.sku = sku;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
