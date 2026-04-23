package fr.arthurbr02.deploymanager.repository;

import fr.arthurbr02.deploymanager.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, String> {}
