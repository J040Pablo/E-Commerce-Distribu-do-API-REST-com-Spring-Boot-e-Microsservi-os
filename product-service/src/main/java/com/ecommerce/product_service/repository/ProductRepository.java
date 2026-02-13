package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para a entidade Product
 * Fornece operações CRUD e queries customizadas
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Busca produto pelo SKU
     */
    Optional<Product> findBySku(String sku);

    /**
     * Busca produtos por categoria
     */
    List<Product> findByCategory(String category);

    /**
     * Busca produtos por nome (case-insensitive)
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Busca produtos com quantidade maior que zero
     */
    @Query("SELECT p FROM Product p WHERE p.quantity > 0")
    List<Product> findAvailableProducts();
}
