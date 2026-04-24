package fr.arthurbr02.deploymanager.security;

import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.repository.UserRepository;
import fr.arthurbr02.deploymanager.service.PersonalAccessTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PersonalAccessTokenService patService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // Fallback for SSE connections (EventSource cannot send custom headers)
            String tokenParam = request.getParameter("token");
            if (tokenParam == null || tokenParam.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }
            token = tokenParam;
        }

        User user = null;

        // Try JWT first
        try {
            Claims claims = jwtUtil.validateAccessToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
        } catch (JwtException | IllegalArgumentException ignored) {
            // If JWT fails, try PAT
            Optional<User> patUser = patService.validateToken(token);
            if (patUser.isPresent()) {
                user = patUser.get();
            }
        }

        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    user, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
