package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.Deployment;
import fr.arthurbr02.deploymanager.entity.Host;
import fr.arthurbr02.deploymanager.repository.HostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AppConfigService configService;
    private final HostRepository hostRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void notifyDeploymentFailure(Deployment deployment) {
        if (!"true".equalsIgnoreCase(configService.get("notification_enabled"))) {
            return;
        }

        String webhookUrl = configService.get("notification_webhook_url");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        Host host = hostRepository.findById(deployment.getHostId()).orElse(null);
        String hostName = host != null ? host.getName() : deployment.getHostId().toString();

        String message = String.format("❌ **Échec du déploiement**\n" +
                "**Hôte** : %s\n" +
                "**Type** : %s\n" +
                "**ID** : %s\n" +
                "Veuillez vérifier les logs pour plus de détails.",
                hostName, deployment.getType(), deployment.getId());

        sendWebhook(webhookUrl, message);
    }

    private void sendWebhook(String url, String content) {
        try {
            Map<String, String> body = Map.of("content", content);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        if (res.statusCode() >= 400) {
                            log.error("Failed to send webhook notification. Status: {}", res.statusCode());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending webhook notification", e);
        }
    }
}
