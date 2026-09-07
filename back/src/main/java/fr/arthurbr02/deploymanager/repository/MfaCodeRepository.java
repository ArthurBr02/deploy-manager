package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.MfaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MfaCodeRepository extends JpaRepository<MfaCode, UUID> {
    Optional<MfaCode> findByChallengeId(UUID challengeId);
    void deleteByUserId(UUID userId);
    void deleteByExpiresAtBefore(Instant cutoff);

    @Modifying
    @Query("update MfaCode c set c.attempts = c.attempts + 1 where c.id = :id")
    void incrementAttempts(@Param("id") UUID id);
}
