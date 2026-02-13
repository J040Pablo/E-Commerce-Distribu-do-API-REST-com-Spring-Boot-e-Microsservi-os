package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductDTO;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para lógica de negócio relacionada a Products
 */
@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Retorna todos os produtos
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        logger.info("Buscando todos os produtos");
        return productRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca produto por ID
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        logger.info("Buscando produto com ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
        return convertToDTO(product);
    }

    /**
     * Busca produtos por categoria
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        logger.info("Buscando produtos da categoria: {}", category);
        return productRepository.findByCategory(category)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca produtos disponíveis (quantidade > 0)
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        logger.info("Buscando produtos disponíveis");
        return productRepository.findAvailableProducts()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cria um novo produto
     */
    public ProductDTO createProduct(ProductDTO productDTO) {
        logger.info("Criando novo produto com SKU: {}", productDTO.getSku());

        // Verifica se já existe produto com o mesmo SKU
        if (productRepository.findBySku(productDTO.getSku()).isPresent()) {
            throw new IllegalArgumentException("Já existe um produto com o SKU: " + productDTO.getSku());
        }

        Product product = new Product(
                productDTO.getName(),
                productDTO.getDescription(),
                productDTO.getPrice(),
                productDTO.getQuantity(),
                productDTO.getSku(),
                productDTO.getCategory()
        );

        Product savedProduct = productRepository.save(product);
        logger.info("Produto criado com sucesso. ID: {}", savedProduct.getId());

        return convertToDTO(savedProduct);
    }

    /**
     * Atualiza um produto existente
     */
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        logger.info("Atualizando produto com ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));

        // Atualiza apenas os campos fornecidos
        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getQuantity() != null) {
            product.setQuantity(productDTO.getQuantity());
        }
        if (productDTO.getCategory() != null) {
            product.setCategory(productDTO.getCategory());
        }

        Product updatedProduct = productRepository.save(product);
        logger.info("Produto atualizado com sucesso. ID: {}", updatedProduct.getId());

        return convertToDTO(updatedProduct);
    }

    /**
     * Deleta um produto
     */
    public void deleteProduct(Long id) {
        logger.info("Deletando produto com ID: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produto não encontrado com ID: " + id);
        }

        productRepository.deleteById(id);
        logger.info("Produto deletado com sucesso. ID: {}", id);
    }

    /**
     * Converte entidade Product para DTO
     */
    private ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getSku(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * Converte DTO para entidade Product
     */
    private Product convertToEntity(ProductDTO productDTO) {
        return new Product(
                productDTO.getName(),
                productDTO.getDescription(),
                productDTO.getPrice(),
                productDTO.getQuantity(),
                productDTO.getSku(),
                productDTO.getCategory()
        );
    }
}
