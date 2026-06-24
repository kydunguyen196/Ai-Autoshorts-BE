package com.autoshorts.ai.service;

import com.autoshorts.ai.entity.AppSetting;
import com.autoshorts.ai.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime-editable configuration overrides. Values are persisted in {@code app_settings}
 * and cached in-memory. Reads fall back to a supplied default (typically an
 * {@link com.autoshorts.ai.config.AppProperties} value sourced from YAML/env) when no
 * override exists, so the system keeps working before any admin changes a setting.
 */
@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final AppSettingRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void warmCache() {
        try {
            reload();
        } catch (Exception ex) {
            log.warn("event=settings_cache_warm_failed message={}", ex.getMessage());
        }
    }

    public void reload() {
        cache.clear();
        for (AppSetting setting : repository.findAll()) {
            if (setting.getValue() != null) {
                cache.put(setting.getKey(), setting.getValue());
            }
        }
    }

    public Optional<String> getRaw(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    public String getString(String key, String fallback) {
        return getRaw(key).filter(v -> !v.isBlank()).orElse(fallback);
    }

    public boolean getBoolean(String key, boolean fallback) {
        return getRaw(key).map(v -> v.trim().equalsIgnoreCase("true") || v.trim().equals("1")).orElse(fallback);
    }

    public int getInt(String key, int fallback) {
        return getRaw(key).map(v -> {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }).orElse(fallback);
    }

    public long getLong(String key, long fallback) {
        return getRaw(key).map(v -> {
            try {
                return Long.parseLong(v.trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }).orElse(fallback);
    }

    public List<AppSetting> listAll() {
        return repository.findAll();
    }

    public List<AppSetting> listByCategory(String category) {
        return repository.findAllByCategoryOrderByKeyAsc(category);
    }

    @Transactional
    public AppSetting update(String key, String value, String valueType, String category, UUID updatedBy) {
        AppSetting setting = repository.findById(key).orElseGet(() -> {
            AppSetting created = new AppSetting();
            created.setKey(key);
            return created;
        });
        setting.setValue(value);
        if (valueType != null && !valueType.isBlank()) {
            setting.setValueType(valueType);
        }
        if (category != null && !category.isBlank()) {
            setting.setCategory(category);
        }
        setting.setUpdatedBy(updatedBy);
        AppSetting saved = repository.save(setting);
        if (value == null) {
            cache.remove(key);
        } else {
            cache.put(key, value);
        }
        log.info("event=setting_updated key={} updatedBy={}", key, updatedBy);
        return saved;
    }
}
