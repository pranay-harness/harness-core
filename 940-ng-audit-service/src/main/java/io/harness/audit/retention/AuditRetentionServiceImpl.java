package io.harness.audit.retention;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.AuditRetention;
import io.harness.audit.repositories.AuditRetentionRepository;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OwnedBy(PL)
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
    if (auditRetention != null) {
      auditRetention.setRetentionPeriodInMonths(months);
      return auditRetentionRepository.save(auditRetention);
    }
    return create(accountIdentifier, months);
  }

  @Override
  public AuditRetention create(String accountIdentifier, int months) {
    AuditRetention auditRetention =
        AuditRetention.builder().accountIdentifier(accountIdentifier).retentionPeriodInMonths(months).build();
    return auditRetentionRepository.save(auditRetention);
  }

  @Override
  public List<AuditRetention> fetchAll() {
    List<AuditRetention> auditRetentionList = new ArrayList<>();
    auditRetentionRepository.findAll().iterator().forEachRemaining(
        auditRetention -> auditRetentionList.add(auditRetention));
    return auditRetentionList;
  }
}
