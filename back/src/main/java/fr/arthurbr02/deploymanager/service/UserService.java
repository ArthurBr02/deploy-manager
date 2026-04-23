package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.dto.user.*;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.enums.Role;
import fr.arthurbr02.deploymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";

    public List<UserResponse> findAll() {
        return userRepository.findAllByDeletedAtIsNull().stream()
                .map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse findById(UUID id) {
        return UserResponse.from(userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable")));
    }

    @Transactional
    public CreateUserResponse create(CreateUserRequest req) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(req.email())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        String tmpPass = generatePassword(16);
        User user = User.builder()
                .email(req.email())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .role(req.role())
                .password(passwordEncoder.encode(tmpPass))
                .build();
        user = userRepository.save(user);
        return new CreateUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name(), tmpPass);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setRole(req.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (user.getRole() == Role.ADMIN && userRepository.countByRoleAndDeletedAtIsNull(Role.ADMIN) <= 1) {
            throw new RuntimeException("Impossible de supprimer le dernier administrateur");
        }
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest req) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setAvatar(req.avatar());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest req) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse uploadAvatar(UUID id, MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("Fichier vide");
        if (file.getSize() > 2 * 1024 * 1024) throw new RuntimeException("Avatar trop volumineux (max 2 Mo)");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Le fichier doit être une image");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            user.setAvatar("data:" + contentType + ";base64," + base64);
            userRepository.save(user);
            return UserResponse.from(user);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier");
        }
    }

    @Transactional
    public UserResponse deleteAvatar(UUID id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setAvatar(null);
        userRepository.save(user);
        return UserResponse.from(user);
    }

    private String generatePassword(int length) {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARSET.charAt(rng.nextInt(CHARSET.length())));
        return sb.toString();
    }
}
