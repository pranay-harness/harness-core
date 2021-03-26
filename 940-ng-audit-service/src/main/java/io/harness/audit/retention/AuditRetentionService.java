package io.harness.audit.retention;

import io.harness.audit.entities.AuditRetention;

import java.util.List;

public interface AuditRetentionService {
  AuditRetention get(String accountIdentifier);
  AuditRetention create(String accountIdentifier, int months);
  AuditRetention update(String accountIdentifier, int months);
  List<AuditRetention> fetchAll();
}
