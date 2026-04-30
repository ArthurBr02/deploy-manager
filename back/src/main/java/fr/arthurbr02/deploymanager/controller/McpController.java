package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.config.AppConfigRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentResponse;
import fr.arthurbr02.deploymanager.dto.host.HostRequest;
import fr.arthurbr02.deploymanager.dto.host.HostWithStatusResponse;
import fr.arthurbr02.deploymanager.dto.host.PermissionRequest;
import fr.arthurbr02.deploymanager.dto.mcp.JsonRpcRequest;
import fr.arthurbr02.deploymanager.dto.mcp.JsonRpcResponse;
import fr.arthurbr02.deploymanager.dto.user.CreateUserRequest;
import fr.arthurbr02.deploymanager.dto.user.UpdateUserRequest;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.enums.DeploymentType;
import fr.arthurbr02.deploymanager.enums.Role;
import fr.arthurbr02.deploymanager.exception.ForbiddenException;
import fr.arthurbr02.deploymanager.service.AppConfigService;
import fr.arthurbr02.deploymanager.service.DeploymentService;
import fr.arthurbr02.deploymanager.service.HostService;
import fr.arthurbr02.deploymanager.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class McpController {

    private final HostService hostService;
    private final DeploymentService deploymentService;
    private final UserService userService;
    private final AppConfigService configService;

    // Map to store SSE emitters by session ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter establishSse(@RequestParam(required = false) String sessionId, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        checkMcpEnabled();
        String id = sessionId != null ? sessionId : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(3600_000L); // 1 hour timeout

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError((e) -> emitters.remove(id));

        emitters.put(id, emitter);

        try {
            // Send the endpoint event as per MCP spec
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/api/mcp/messages?sessionId=" + id));
        } catch (IOException e) {
            log.error("Failed to send endpoint event", e);
        }

        return emitter;
    }

    @PostMapping("/mcp/messages")
    public ResponseEntity<Void> handleMessage(
            @RequestParam String sessionId,
            @RequestBody JsonRpcRequest request,
            @AuthenticationPrincipal User user) {
        checkMcpEnabled();
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            return ResponseEntity.notFound().build();
        }

        // Process in background to not block the POST
        new Thread(() -> {
            JsonRpcResponse response = processRequest(request, user);
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(response));
            } catch (IOException e) {
                log.error("Failed to send response via SSE", e);
            }
        }).start();

        return ResponseEntity.accepted().build();
    }

    private void checkMcpEnabled() {
        if (!"true".equals(configService.get("mcp_enabled", "true"))) {
            throw new ForbiddenException("MCP est désactivé");
        }
    }

    private JsonRpcResponse processRequest(JsonRpcRequest request, User user) {
        try {
            Object result = null;
            switch (request.getMethod()) {
                case "listTools":
                    result = Map.of("tools", listTools(user));
                    break;

                case "callTool":
                    String toolName = (String) request.getParams().get("name");
                    Map<String, Object> arguments = (Map<String, Object>) request.getParams().get("arguments");
                    result = executeTool(toolName, arguments, user);
                    break;

                default:
                    return JsonRpcResponse.builder()
                            .jsonrpc("2.0")
                            .id(request.getId())
                            .error(Map.of("code", -32601, "message", "Method not found"))
                            .build();
            }

            return JsonRpcResponse.builder()
                    .jsonrpc("2.0")
                    .id(request.getId())
                    .result(result)
                    .build();

        } catch (Exception e) {
            log.error("Error processing MCP request", e);
            return JsonRpcResponse.builder()
                    .jsonrpc("2.0")
                    .id(request.getId())
                    .error(Map.of("code", -32603, "message", e.getMessage()))
                    .build();
        }
    }

    private List<Map<String, Object>> listTools(User user) {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // list_hosts
        tools.add(Map.of("name", "list_hosts", "description", "Liste les serveurs accessibles", "inputSchema", Map.of("type", "object", "properties", Map.of())));
        
        // get_host
        tools.add(Map.of("name", "get_host", "description", "Détails d'un serveur", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string")))));
        
        // update_host
        Map<String, Object> updateHostProps = new LinkedHashMap<>();
        updateHostProps.put("hostId", Map.of("type", "string"));
        updateHostProps.put("name", Map.of("type", "string"));
        updateHostProps.put("ip", Map.of("type", "string"));
        updateHostProps.put("domain", Map.of("type", "string"));
        updateHostProps.put("sshUser", Map.of("type", "string"));
        updateHostProps.put("sshPort", Map.of("type", "integer"));
        updateHostProps.put("deploymentCommand", Map.of("type", "string"));
        updateHostProps.put("generateCommand", Map.of("type", "string"));
        updateHostProps.put("deliverCommand", Map.of("type", "string"));
        updateHostProps.put("tlogCommand", Map.of("type", "string"));
        updateHostProps.put("rollbackCommand", Map.of("type", "string"));
        updateHostProps.put("healthcheckUrl", Map.of("type", "string"));
        updateHostProps.put("dumpFolder", Map.of("type", "string"));
        updateHostProps.put("defaultTimeout", Map.of("type", "integer"));
        tools.add(Map.of("name", "update_host", "description", "Modifier un serveur", "inputSchema", Map.of("type", "object", "properties", updateHostProps)));

        // deploy
        tools.add(Map.of("name", "deploy", "description", "Lancer un déploiement", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string"), "type", Map.of("type", "string", "enum", List.of("DEPLOY", "GENERATE", "DELIVER", "ROLLBACK")), "timeout", Map.of("type", "integer", "description", "Timeout en minutes (optionnel)")))));
        
        // get_deployments
        tools.add(Map.of("name", "get_deployments", "description", "Historique des déploiements", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string")))));

        if (user.getRole() == Role.ADMIN) {
            // create_host
            Map<String, Object> createHostProps = new LinkedHashMap<>(updateHostProps);
            createHostProps.remove("hostId");
            tools.add(Map.of("name", "create_host", "description", "Créer un serveur (Admin)", "inputSchema", Map.of("type", "object", "properties", createHostProps)));
            
            tools.add(Map.of("name", "delete_host", "description", "Supprimer un serveur (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string")))));
            tools.add(Map.of("name", "list_users", "description", "Lister les utilisateurs (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of())));
            tools.add(Map.of("name", "create_user", "description", "Créer un utilisateur (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of("email", Map.of("type", "string"), "firstName", Map.of("type", "string"), "lastName", Map.of("type", "string"), "role", Map.of("type", "string", "enum", List.of("USER", "ADMIN"))))));
            tools.add(Map.of("name", "update_user", "description", "Modifier un utilisateur (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string"), "firstName", Map.of("type", "string"), "lastName", Map.of("type", "string"), "role", Map.of("type", "string", "enum", List.of("USER", "ADMIN"))))));
            tools.add(Map.of("name", "delete_user", "description", "Supprimer un utilisateur (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string")))));
            
            // set_permissions
            Map<String, Object> permProps = new LinkedHashMap<>();
            permProps.put("userId", Map.of("type", "string"));
            permProps.put("hostId", Map.of("type", "string"));
            permProps.put("canDeploy", Map.of("type", "boolean"));
            permProps.put("canEdit", Map.of("type", "boolean"));
            permProps.put("canExecute", Map.of("type", "boolean"));
            permProps.put("canDump", Map.of("type", "boolean"));
            tools.add(Map.of("name", "set_permissions", "description", "Gérer les permissions (Admin)", "inputSchema", Map.of("type", "object", "properties", permProps)));
            
            tools.add(Map.of("name", "get_settings", "description", "Voir les paramètres globaux (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of())));
            tools.add(Map.of("name", "update_settings", "description", "Modifier les paramètres globaux (Admin)", "inputSchema", Map.of("type", "object", "properties", Map.of("settings", Map.of("type", "object", "additionalProperties", Map.of("type", "string"))))));
        }

        return tools;
    }

    private Object executeTool(String name, Map<String, Object> args, User user) {
        switch (name) {
            case "list_hosts":
                return textResponse(hostService.findAll(user));

            case "get_host":
                return textResponse(hostService.findById(UUID.fromString((String) args.get("hostId")), user));

            case "create_host":
                checkAdmin(user);
                return textResponse(hostService.create(mapToHostRequest(args)));

            case "update_host":
                UUID uHostId = UUID.fromString((String) args.get("hostId"));
                return textResponse(hostService.update(uHostId, mapToHostRequest(args), user));

            case "delete_host":
                checkAdmin(user);
                hostService.delete(UUID.fromString((String) args.get("hostId")), user);
                return textResponse("Hôte supprimé");

            case "deploy":
                UUID dHostId = UUID.fromString((String) args.get("hostId"));
                DeploymentType dType = DeploymentType.valueOf((String) args.get("type"));
                Integer timeout = (Integer) args.get("timeout");
                if (timeout == null) {
                    // Try to get host's default timeout
                    HostWithStatusResponse host = hostService.findById(dHostId, user);
                    timeout = host.defaultTimeout() != null ? host.defaultTimeout() : 0;
                }
                DeploymentResponse resp = deploymentService.launch(dHostId, new DeploymentRequest(dType, timeout), user);
                return textResponse("Déploiement lancé avec l'ID: " + resp.id());

            case "get_deployments":
                return textResponse("Utilisez l'API web pour consulter l'historique détaillé.");

            case "list_users":
                checkAdmin(user);
                return textResponse(userService.findAll());

            case "create_user":
                checkAdmin(user);
                return textResponse(userService.create(new CreateUserRequest(
                        (String) args.get("email"),
                        (String) args.get("firstName"),
                        (String) args.get("lastName"),
                        Role.valueOf((String) args.get("role"))
                )));

            case "update_user":
                checkAdmin(user);
                return textResponse(userService.update(UUID.fromString((String) args.get("id")), new UpdateUserRequest(
                        (String) args.get("firstName"),
                        (String) args.get("lastName"),
                        Role.valueOf((String) args.get("role"))
                )));

            case "delete_user":
                checkAdmin(user);
                userService.delete(UUID.fromString((String) args.get("id")), user);
                return textResponse("Utilisateur supprimé");

            case "set_permissions":
                checkAdmin(user);
                hostService.setPermission(UUID.fromString((String) args.get("userId")), new PermissionRequest(
                        UUID.fromString((String) args.get("hostId")),
                        (boolean) args.get("canDeploy"),
                        (boolean) args.get("canEdit"),
                        args.containsKey("canExecute") ? (boolean) args.get("canExecute") : false,
                        args.containsKey("canDump") ? (boolean) args.get("canDump") : false
                ));
                return textResponse("Permissions mises à jour");

            case "get_settings":
                checkAdmin(user);
                return textResponse(configService.getAll());

            case "update_settings":
                checkAdmin(user);
                configService.saveAll((Map<String, String>) args.get("settings"));
                return textResponse("Paramètres mis à jour");

            default:
                throw new RuntimeException("Outil inconnu: " + name);
        }
    }

    private void checkAdmin(User user) {
        if (user.getRole() != Role.ADMIN) throw new RuntimeException("Action réservée aux administrateurs");
    }

    private Object textResponse(Object data) {
        return Map.of("content", List.of(Map.of("type", "text", "text", data.toString())));
    }

    private HostRequest mapToHostRequest(Map<String, Object> args) {
        return new HostRequest(
                (String) args.get("name"),
                (String) args.get("ip"),
                (String) args.get("domain"),
                (String) args.get("sshUser"),
                (Integer) args.get("sshPort"),
                (String) args.get("deploymentCommand"),
                (String) args.get("generateCommand"),
                (String) args.get("deliverCommand"),
                (String) args.get("tlogCommand"),
                (String) args.get("rollbackCommand"),
                (String) args.get("healthcheckUrl"),
                (String) args.get("dumpCommand"),
                (String) args.get("dumpFolder"),
                args.get("dumpEnabled") instanceof Boolean b ? b : null,
                (String) args.get("dumpFilename"),
                (Integer) args.get("defaultTimeout")
        );
    }
}
