package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.MfaCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MfaCodeRepository extends JpaRepository<MfaCode, UUID> {
    Optional<MfaCode> findByChallengeId(UUID challengeId);
    void deleteByUserId(UUID userId);
    void deleteByExpiresAtBefore(Instant cutoff);
}
