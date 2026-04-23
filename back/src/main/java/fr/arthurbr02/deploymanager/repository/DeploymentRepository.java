package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.Deployment;
import fr.arthurbr02.deploymanager.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID>, JpaSpecificationExecutor<Deployment> {
    @Query("SELECT d FROM Deployment d WHERE d.hostId = :hostId ORDER BY d.createdAt DESC LIMIT 1")
    Optional<Deployment> findLastByHostId(UUID hostId);

    List<Deployment> findByHostIdAndStatus(UUID hostId, DeploymentStatus status);
    boolean existsByHostIdAndStatus(UUID hostId, DeploymentStatus status);

    @Query("SELECT d FROM Deployment d WHERE d.hostId IN :hostIds ORDER BY d.createdAt DESC")
    Page<Deployment> findByHostIdIn(List<UUID> hostIds, Pageable pageable);
}
