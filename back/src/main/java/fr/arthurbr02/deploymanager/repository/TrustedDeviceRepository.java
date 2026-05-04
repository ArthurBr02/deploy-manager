package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {
    List<TrustedDevice> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<TrustedDevice> findByIdAndUserId(UUID id, UUID userId);
    void deleteByUserId(UUID userId);
    void deleteByExpiresAtBefore(Instant cutoff);
}
