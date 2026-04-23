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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
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

    private static final int AVATAR_MAX_DIMENSION = 256;
    private static final float AVATAR_JPEG_QUALITY = 0.80f;

    @Transactional
    public UserResponse uploadAvatar(UUID id, MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("Fichier vide");
        if (file.getSize() > 10 * 1024 * 1024) throw new RuntimeException("Avatar trop volumineux (max 10 Mo)");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Le fichier doit être une image");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        try {
            byte[] compressed = compressImage(file.getBytes());
            String base64 = Base64.getEncoder().encodeToString(compressed);
            user.setAvatar("data:image/jpeg;base64," + base64);
            userRepository.save(user);
            return UserResponse.from(user);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du traitement de l'image");
        }
    }

    private byte[] compressImage(byte[] input) throws IOException {
        BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(input));
        if (original == null) throw new RuntimeException("Format d'image non supporté");

        int w = original.getWidth();
        int h = original.getHeight();
        if (w > AVATAR_MAX_DIMENSION || h > AVATAR_MAX_DIMENSION) {
            double scale = (double) AVATAR_MAX_DIMENSION / Math.max(w, h);
            w = (int) (w * scale);
            h = (int) (h * scale);
        }

        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, w, h, null);
        g.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new RuntimeException("Encodeur JPEG indisponible");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(AVATAR_JPEG_QUALITY);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(resized, null, null), param);
        writer.dispose();
        return out.toByteArray();
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
