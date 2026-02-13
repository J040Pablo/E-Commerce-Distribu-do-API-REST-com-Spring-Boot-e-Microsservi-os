package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para a entidade User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca usuário pelo username
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca usuário pelo email
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se username já existe
     */
    boolean existsByUsername(String username);

    /**
     * Verifica se email já existe
     */
    boolean existsByEmail(String email);
}
