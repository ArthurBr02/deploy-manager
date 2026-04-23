package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.UserHostPermission;
import fr.arthurbr02.deploymanager.entity.UserHostPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserHostPermissionRepository extends JpaRepository<UserHostPermission, UserHostPermissionId> {
    List<UserHostPermission> findByUserId(UUID userId);
    List<UserHostPermission> findByHostId(UUID hostId);
    Optional<UserHostPermission> findByUserIdAndHostId(UUID userId, UUID hostId);
    void deleteByUserIdAndHostId(UUID userId, UUID hostId);
}
