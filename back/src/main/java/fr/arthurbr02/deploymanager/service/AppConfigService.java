package fr.arthurbr02.deploymanager.service;

import fr.arthurbr02.deploymanager.entity.AppConfig;
import fr.arthurbr02.deploymanager.repository.AppConfigRepository;
import fr.arthurbr02.deploymanager.util.AuditConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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

    public boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(get(key));
    }

    public void saveAll(Map<String, String> settings) {
        Map<String, String> oldSnapshot = new LinkedHashMap<>();
        Map<String, String> newSnapshot = new LinkedHashMap<>();

        settings.forEach((k, v) -> {
            AppConfig c = configRepository.findById(k)
                    .orElse(AppConfig.builder().key(k).build());
            String oldValue = c.getValue();
            if (oldValue == null || !oldValue.equals(v)) {
                c.setValue(v);
                configRepository.save(c);
                oldSnapshot.put(k, mask(k, oldValue));
                newSnapshot.put(k, mask(k, v));
            }
        });

        if (!oldSnapshot.isEmpty()) {
            auditService.log(AuditConstants.ENTITY_APP_CONFIG, null, AuditConstants.ACTION_UPDATE, oldSnapshot, newSnapshot);
        }
    }

    private static String mask(String key, String value) {
        if (key != null && key.toLowerCase().contains("password") && value != null && !value.isBlank()) {
            return "***";
        }
        return value;
    }
}
