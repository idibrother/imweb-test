package com.imweb.shop.auth.application;

import com.imweb.shop.auth.domain.AuditLog;
import com.imweb.shop.auth.infrastructure.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String action, Long userId, String username, String ipAddress, String userAgent, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .build());
    }
}
