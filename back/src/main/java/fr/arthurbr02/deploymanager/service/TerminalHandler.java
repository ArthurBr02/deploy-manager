package fr.arthurbr02.deploymanager.service;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import fr.arthurbr02.deploymanager.entity.Host;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.entity.UserHostPermission;
import fr.arthurbr02.deploymanager.enums.Role;
import fr.arthurbr02.deploymanager.repository.HostRepository;
import fr.arthurbr02.deploymanager.repository.UserHostPermissionRepository;
import fr.arthurbr02.deploymanager.repository.UserRepository;
import fr.arthurbr02.deploymanager.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerminalHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final HostRepository hostRepository;
    private final UserHostPermissionRepository permissionRepository;
    private final AppConfigService configService;
    
    private final Map<String, SshSession> sshSessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private record SshSession(Session session, ChannelShell channel, OutputStream out) {}

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        Map<String, String> params = parseQueryParams(wsSession.getUri().getQuery());
        String token = params.get("token");
        String hostIdStr = params.get("hostId");

        if (token == null || hostIdStr == null) {
            wsSession.close(CloseStatus.BAD_DATA);
            return;
        }

        User user;
        try {
            Claims claims = jwtUtil.validateAccessToken(token);
            String userIdStr = claims.getSubject();
            user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(userIdStr)).orElse(null);
        } catch (Exception e) {
            wsSession.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        if (user == null) {
            wsSession.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        UUID hostId = UUID.fromString(hostIdStr);
        Host host = hostRepository.findByIdAndDeletedAtIsNull(hostId).orElse(null);
        if (host == null) {
            wsSession.close(CloseStatus.BAD_DATA);
            return;
        }

        if (user.getRole() != Role.ADMIN) {
            UserHostPermission perm = permissionRepository.findByUserIdAndHostId(user.getId(), hostId).orElse(null);
            if (perm == null || !perm.isCanExecute()) {
                wsSession.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
        }

        // Connect to SSH
        try {
            JSch jsch = new JSch();
            
            String sshKeyPath = configService.get("ssh_key_path", null);
            if (sshKeyPath != null && !sshKeyPath.isBlank()) {
                jsch.addIdentity(sshKeyPath);
            }

            // Fallback to IP if domain is blank
            String targetHost = (host.getDomain() != null && !host.getDomain().isBlank()) ? host.getDomain() : host.getIp();
            
            Session session = jsch.getSession("root", targetHost, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            
            session.connect(30000);
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setPty(true);
            channel.connect();

            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();

            sshSessions.put(wsSession.getId(), new SshSession(session, channel, out));

            executor.execute(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int i;
                    while ((i = in.read(buffer)) != -1) {
                        if (wsSession.isOpen()) {
                            wsSession.sendMessage(new TextMessage(new String(buffer, 0, i)));
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading from SSH", e);
                } finally {
                    try { wsSession.close(); } catch (Exception ignored) {}
                }
            });

        } catch (Exception e) {
            log.error("Failed to connect SSH for terminal", e);
            wsSession.sendMessage(new TextMessage("\r\n[ERROR] Impossible de se connecter en SSH : " + e.getMessage() + "\r\n"));
            wsSession.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        SshSession ssh = sshSessions.get(wsSession.getId());
        if (ssh != null && ssh.out != null) {
            ssh.out.write(message.getPayload().getBytes());
            ssh.out.flush();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
        SshSession ssh = sshSessions.remove(wsSession.getId());
        if (ssh != null) {
            if (ssh.channel != null) ssh.channel.disconnect();
            if (ssh.session != null) ssh.session.disconnect();
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) params.put(kv[0], kv[1]);
            }
        }
        return params;
    }
}
