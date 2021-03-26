package io.harness.audit.retention;

import io.harness.audit.entities.AuditRetention;

public interface AuditRetentionService {
   AuditRetention get(String accountIdentifier);
    AuditRetention create(String accountIdentifier);
    AuditRetention update(String accountIdentifier, int months);

   }
