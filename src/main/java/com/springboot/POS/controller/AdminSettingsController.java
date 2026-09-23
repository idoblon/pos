package com.springboot.POS.controller;

import com.springboot.POS.modal.SystemSetting;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.SystemSettingRepository;
import com.springboot.POS.service.AdminAuditService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Real system settings store. The admin UI previously kept every toggle in
 * localStorage (placebo). Settings saved here are the enforced values;
 * the UI keeps localStorage only for client polling intervals.
 * Showcase-safe: unknown keys are stored as-is; reads never fail.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final SystemSettingRepository repository;
    private final UserService userService;
    private final AdminAuditService auditService;

    @GetMapping
    public ResponseEntity<Map<String, String>> all() {
        Map<String, String> out = repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s.getSettingValue() == null ? "" : s.getSettingValue(),
                        (a, b) -> b, LinkedHashMap::new));
        return ResponseEntity.ok(out);
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> save(
            @RequestBody Map<String, String> settings,
            @RequestHeader("Authorization") String jwt) {
        User admin = userService.getUserFromJwtToken(jwt);
        Map<String, String> saved = new LinkedHashMap<>();
        settings.forEach((key, value) -> {
            if (key == null || key.isBlank() || key.length() > 128) return;
            String v = value == null ? "" : (value.length() > 4000 ? value.substring(0, 4000) : value);
            repository.save(SystemSetting.builder()
                    .settingKey(key.trim())
                    .settingValue(v)
                    .updatedBy(admin.getId())
                    .build());
            saved.put(key.trim(), v);
        });
        auditService.record(admin.getId(), "SETTINGS_UPDATE", "settings", null,
                "Updated " + saved.size() + " setting(s)");
        return ResponseEntity.ok(saved);
    }
}
