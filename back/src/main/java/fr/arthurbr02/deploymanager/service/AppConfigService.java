package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.AppConfig;
import fr.arthurbr02.deploymanager.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository configRepository;
    private final AuditService auditService;

    public Map<String, String> getAll() {
        return configRepository.findAll().stream()
                .collect(Collectors.toMap(AppConfig::getKey, c -> c.getValue() != null ? c.getValue() : ""));
    }

    public String get(String key) {
        return configRepository.findById(key).map(AppConfig::getValue).orElse(null);
    }

    public String get(String key, String defaultValue) {
        String v = get(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }

    public void saveAll(Map<String, String> settings) {
        settings.forEach((k, v) -> {
            AppConfig c = configRepository.findById(k)
                    .orElse(AppConfig.builder().key(k).build());
            String oldValue = c.getValue();
            if (oldValue == null || !oldValue.equals(v)) {
                c.setValue(v);
                configRepository.save(c);
                auditService.log("AppConfig", null, "UPDATE", k + "=" + oldValue, k + "=" + v);
            }
        });
    }
}
