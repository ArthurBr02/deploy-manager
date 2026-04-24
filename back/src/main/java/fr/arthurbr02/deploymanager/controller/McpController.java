package fr.arthurbr02.deploymanager.controller;

import fr.arthurbr02.deploymanager.dto.deployment.DeploymentRequest;
import fr.arthurbr02.deploymanager.dto.deployment.DeploymentResponse;
import fr.arthurbr02.deploymanager.dto.host.HostWithStatusResponse;
import fr.arthurbr02.deploymanager.dto.mcp.JsonRpcRequest;
import fr.arthurbr02.deploymanager.dto.mcp.JsonRpcResponse;
import fr.arthurbr02.deploymanager.entity.User;
import fr.arthurbr02.deploymanager.enums.DeploymentType;
import fr.arthurbr02.deploymanager.service.DeploymentService;
import fr.arthurbr02.deploymanager.service.HostService;
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

    // Map to store SSE emitters by session ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/mcp/sse")
    public SseEmitter establishSse(@RequestParam(required = false) String sessionId) {
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

    private JsonRpcResponse processRequest(JsonRpcRequest request, User user) {
        try {
            Object result = null;
            switch (request.getMethod()) {
                case "listTools":
                    result = Map.of("tools", List.of(
                            Map.of("name", "list_hosts", "description", "Liste les serveurs accessibles", "inputSchema", Map.of("type", "object", "properties", Map.of())),
                            Map.of("name", "get_host", "description", "Détails d'un serveur", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string")))),
                            Map.of("name", "deploy", "description", "Lancer un déploiement", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string"), "type", Map.of("type", "string", "enum", List.of("ALL", "FRONT", "BACK"))))),
                            Map.of("name", "get_deployments", "description", "Historique des déploiements", "inputSchema", Map.of("type", "object", "properties", Map.of("hostId", Map.of("type", "string"))))
                    ));
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
            return JsonRpcResponse.builder()
                    .jsonrpc("2.0")
                    .id(request.getId())
                    .error(Map.of("code", -32603, "message", e.getMessage()))
                    .build();
        }
    }

    private Object executeTool(String name, Map<String, Object> args, User user) {
        switch (name) {
            case "list_hosts":
                return Map.of("content", List.of(Map.of("type", "text", "text", hostService.findAll(user).toString())));

            case "get_host":
                UUID hostId = UUID.fromString((String) args.get("hostId"));
                return Map.of("content", List.of(Map.of("type", "text", "text", hostService.findById(hostId, user).toString())));

            case "deploy":
                UUID dHostId = UUID.fromString((String) args.get("hostId"));
                DeploymentType dType = DeploymentType.valueOf((String) args.get("type"));
                DeploymentResponse resp = deploymentService.deploy(dHostId, new DeploymentRequest(dType), user);
                return Map.of("content", List.of(Map.of("type", "text", "text", "Déploiement lancé avec l'ID: " + resp.id())));

            case "get_deployments":
                // Basic list, could be improved with pagination
                return Map.of("content", List.of(Map.of("type", "text", "text", "Historique non implémenté en détail via MCP encore")));

            default:
                throw new RuntimeException("Outil inconnu: " + name);
        }
    }
}
