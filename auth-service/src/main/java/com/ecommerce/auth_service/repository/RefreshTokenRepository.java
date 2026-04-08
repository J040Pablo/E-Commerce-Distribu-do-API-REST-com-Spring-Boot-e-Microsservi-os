package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJwtId(String jwtId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken rt
               set rt.revoked = true,
                   rt.revokedAt = :now,
                   rt.revokeReason = :reason
             where rt.user.id = :userId
               and rt.revoked = false
               and rt.expiryDate > :now
            """)
    int revokeAllActiveByUserId(@Param("userId") Long userId,
                                @Param("reason") String reason,
                                @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiryDate <= :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
