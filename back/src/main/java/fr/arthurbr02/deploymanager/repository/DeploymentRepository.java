package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.Deployment;
import fr.arthurbr02.deploymanager.enums.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    long countByCreatedAtAfter(LocalDateTime since);
    long countByCreatedAtAfterAndStatus(LocalDateTime since, DeploymentStatus status);
    long countByStatus(DeploymentStatus status);

    @Query(value = "SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (finished_at - created_at))), 0) FROM deployments WHERE finished_at IS NOT NULL AND status = 'SUCCESS' AND (CAST(:since AS timestamp) IS NULL OR created_at > CAST(:since AS timestamp))", nativeQuery = true)
    Double medianDurationSeconds(@Param("since") LocalDateTime since);

    @Query(value = """
        SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (finished_at - created_at))), 0)
        FROM deployments
        WHERE finished_at IS NOT NULL AND status = 'SUCCESS'
        AND (CAST(:since AS timestamp) IS NULL OR created_at > CAST(:since AS timestamp))
        AND (CAST(:hostId AS uuid) IS NULL OR host_id = CAST(:hostId AS uuid))
        AND (CAST(:type AS varchar) IS NULL OR type = CAST(:type AS varchar))
        """, nativeQuery = true)
    Double medianDurationFiltered(
        @Param("since") LocalDateTime since,
        @Param("hostId") UUID hostId,
        @Param("type") String type
    );
}
