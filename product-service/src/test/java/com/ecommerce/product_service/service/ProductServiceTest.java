package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.exception.BusinessException;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDTO testProductDTO;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setSku("SKU-001");
        testProduct.setName("Laptop");
        testProduct.setCategory("Electronics");
        testProduct.setPrice(BigDecimal.valueOf(999.99));
        testProduct.setQuantity(50);
        testProduct.setDescription("High-performance laptop");

        testProductDTO = new ProductDTO();
        testProductDTO.setId(1L);
        testProductDTO.setSku("SKU-001");
        testProductDTO.setName("Laptop");
        testProductDTO.setCategory("Electronics");
        testProductDTO.setPrice(BigDecimal.valueOf(999.99));
        testProductDTO.setQuantity(50);
        testProductDTO.setDescription("High-performance laptop");
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProductSuccess() {
        // Arrange
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        ProductDTO response = productService.createProduct(testProductDTO);

        // Assert
        assertThat(response)
            .as("Created product response should not be null")
            .isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-001");
        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getQuantity()).isEqualTo(50);

        verify(productRepository).existsBySku("SKU-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should fail when creating product with duplicate SKU")
    void testCreateProductDuplicateSku() {
        // Arrange
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(testProductDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU");

        verify(productRepository).existsBySku("SKU-001");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProductSuccess() {
        // Arrange
        testProductDTO.setPrice(BigDecimal.valueOf(899.99));
        testProductDTO.setQuantity(40);
        
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setSku("SKU-001");
        updatedProduct.setName("Laptop");
        updatedProduct.setPrice(BigDecimal.valueOf(899.99));
        updatedProduct.setQuantity(40);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // Act
        ProductDTO response = productService.updateProduct(1L, testProductDTO);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(899.99));
        assertThat(response.getQuantity()).isEqualTo(40);

        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should fail to update product when product not found")
    void testUpdateProductNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(999L, testProductDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não encontrado");

        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve product by ID successfully")
    void testGetProductByIdSuccess() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        ProductDTO response = productService.getProductById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");

        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void testGetProductByIdNotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não encontrado");

        verify(productRepository).findById(999L);
    }

    @Test
    @DisplayName("Should handle product not found gracefully")
    void testProductNotFoundHandling() {
        // Arrange
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should update product quantity successfully")
    void testUpdateProductQuantitySuccess() {
        // Arrange
        ProductDTO request = new ProductDTO();
        request.setQuantity(30);

        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setSku("SKU-001");
        updatedProduct.setName("Laptop");
        updatedProduct.setDescription("High-performance laptop");
        updatedProduct.setCategory("Electronics");
        updatedProduct.setPrice(BigDecimal.valueOf(999.99));
        updatedProduct.setQuantity(30);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // Act
        ProductDTO response = productService.updateProduct(1L, request);

        // Assert
        assertThat(response)
                .as("Updated product response should not be null")
                .isNotNull();
        assertThat(response.getQuantity())
                .as("Quantity should be updated to the requested value")
                .isEqualTo(30);

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should retrieve all available products")
    void testGetAvailableProducts() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setQuantity(50);
        product1.setName("Laptop");
        product1.setDescription("Gaming laptop");
        product1.setCategory("Electronics");
        product1.setSku("SKU-001");
        product1.setPrice(BigDecimal.valueOf(999.99));
        
        Product product2 = new Product();
        product2.setId(2L);
        product2.setQuantity(30);
        product2.setName("Mouse");
        product2.setDescription("Wireless mouse");
        product2.setCategory("Electronics");
        product2.setSku("SKU-002");
        product2.setPrice(BigDecimal.valueOf(99.99));
        
        when(productRepository.findAvailableProducts()).thenReturn(Arrays.asList(product1, product2));

        // Act
        List<ProductDTO> response = productService.getAvailableProducts();

        // Assert
        assertThat(response)
            .as("Available products list should return mapped products")
            .isNotNull()
            .hasSize(2);

        verify(productRepository).findAvailableProducts();
    }

    @Test
    @DisplayName("Should retrieve products by category")
    void testGetProductsByCategory() {
        // Arrange
        when(productRepository.findByCategory("Electronics")).thenReturn(Arrays.asList(testProduct));

        // Act
        List<ProductDTO> response = productService.getProductsByCategory("Electronics");

        // Assert
        assertThat(response).isNotNull().hasSize(1);
        assertThat(response.get(0).getCategory()).isEqualTo("Electronics");

        verify(productRepository).findByCategory("Electronics");
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProductSuccess() {
        // Arrange
        when(productRepository.existsById(1L)).thenReturn(true);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should fail to delete non-existent product")
    void testDeleteProductNotFound() {
        // Arrange
        when(productRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository).existsById(999L);
        verify(productRepository, never()).deleteById(anyLong());
    }
}
