package io.harness.audit.retention;

import io.harness.audit.entities.AuditRetention;
import io.harness.audit.repositories.AuditRetentionRepository;

import com.google.inject.Inject;
import java.util.Optional;

public class AuditRetentionServiceImpl implements AuditRetentionService {
  @Inject private AuditRetentionRepository auditRetentionRepository;

  @Override
  public AuditRetention get(String accountIdentifier) {
    Optional<AuditRetention> optionalAuditRetention =
        auditRetentionRepository.findByAccountIdentifier(accountIdentifier);
    return optionalAuditRetention.orElse(null);
  }

  @Override
  public AuditRetention update(String accountIdentifier, int months) {
    AuditRetention auditRetention = get(accountIdentifier);
    auditRetention.setRetentionPeriodInMonths(months);
    return auditRetentionRepository.save(auditRetention);
  }

  @Override
  public AuditRetention create(String accountIdentifier) {
    if (get(accountIdentifier) == null) {
      AuditRetention auditRetention =
          AuditRetention.builder().accountIdentifier(accountIdentifier).retentionPeriodInMonths(24).build();
      return auditRetentionRepository.save(auditRetention);
    }
    return null;
  }
}
