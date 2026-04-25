package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
    List<User> findAllByDeletedAtIsNull();
    long countByRoleAndDeletedAtIsNull(Role role);
    List<User> findAllByRoleAndDeletedAtIsNull(Role role);
    boolean existsByEmailAndDeletedAtIsNull(String email);
}
