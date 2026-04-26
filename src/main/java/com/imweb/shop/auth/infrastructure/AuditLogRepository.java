package com.imweb.shop.auth.infrastructure;

import com.imweb.shop.auth.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
