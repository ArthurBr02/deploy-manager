package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.PersonalAccessToken;
import fr.arthurbr02.deploymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonalAccessTokenRepository extends JpaRepository<PersonalAccessToken, UUID> {
    Optional<PersonalAccessToken> findByToken(String token);
    List<PersonalAccessToken> findByUser(User user);
    List<PersonalAccessToken> findByUserId(UUID userId);
}
