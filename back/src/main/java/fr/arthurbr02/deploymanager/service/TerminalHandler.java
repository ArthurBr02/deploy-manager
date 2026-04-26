package fr.arthurbr02.deploymanager.service;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
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
            
            String sshKeyPath = configService.get("ssh_key_path", "/root/.ssh/id_rsa");
            log.info("Using SSH identity: {}", sshKeyPath);
            File keyFile = new File(sshKeyPath);
            if (!keyFile.exists()) {
                log.warn("SSH identity file not found: {}", sshKeyPath);
                wsSession.sendMessage(new TextMessage("\r\n[WARN] Clé SSH non trouvée : " + sshKeyPath + "\r\n"));
            } else {
                jsch.addIdentity(sshKeyPath);
            }

            // Fallback to IP if domain is blank
            String targetHost = (host.getDomain() != null && !host.getDomain().isBlank()) ? host.getDomain() : host.getIp();
            String sshUser = (host.getSshUser() != null && !host.getSshUser().isBlank()) ? host.getSshUser() : "root";
            int sshPort = (host.getSshPort() != null && host.getSshPort() > 0) ? host.getSshPort() : 22;
            
            log.info("Connecting to SSH: {}@{}:{}", sshUser, targetHost, sshPort);
            Session session = jsch.getSession(sshUser, targetHost, sshPort);
            session.setConfig("StrictHostKeyChecking", "no");
            
            session.connect(30000);
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setPty(true);
            channel.connect();

            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();

            sshSessions.put(wsSession.getId(), new SshSession(session, channel, out));
            wsSession.sendMessage(new TextMessage("*** Session SSH établie ***\r\n"));

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

        } catch (JSchException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Auth fail") || msg.contains("USERAUTH fail")) {
                log.error("SSH auth failed for terminal (no valid key configured)", e);
                wsSession.sendMessage(new TextMessage(
                    "\r\n[ERREUR] Authentification SSH échouée.\r\n" +
                    "Aucune clé SSH valide n'est configurée.\r\n" +
                    "Un administrateur doit renseigner le chemin de la clé SSH dans Paramètres > Clé SSH.\r\n"
                ));
            } else {
                log.error("Failed to connect SSH for terminal", e);
                wsSession.sendMessage(new TextMessage("\r\n[ERREUR] Impossible de se connecter en SSH : " + msg + "\r\n"));
            }
            wsSession.close(CloseStatus.SERVER_ERROR);
        } catch (Exception e) {
            log.error("Failed to connect SSH for terminal", e);
            wsSession.sendMessage(new TextMessage("\r\n[ERREUR] Impossible de se connecter en SSH : " + e.getMessage() + "\r\n"));
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
