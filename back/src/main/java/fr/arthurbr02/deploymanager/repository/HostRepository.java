package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HostRepository extends JpaRepository<Host, UUID> {
    List<Host> findAllByDeletedAtIsNull();
    Optional<Host> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Host> findByNameAndDeletedAtIsNull(String name);

    @Query("SELECT h FROM Host h WHERE h.deletedAt IS NULL AND h.id IN " +
           "(SELECT p.hostId FROM UserHostPermission p WHERE p.userId = :userId AND (p.canDeploy = true OR p.canEdit = true))")
    List<Host> findAccessibleByUserId(UUID userId);
}
