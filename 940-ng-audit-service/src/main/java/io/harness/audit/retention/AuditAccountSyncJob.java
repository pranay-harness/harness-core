package io.harness.audit.retention;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.entities.AuditRetention;

import com.google.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AuditAccountSyncJob implements Runnable {
  @Inject private AuditService auditService;
  @Inject private AuditRetentionService auditRetentionService;

  @Override
  public void run() {
    try {
      Set<String> accountIdentifiers = auditService.fetchDistinctAccounts();
      Set<String> alreadyPresentAccountIdentifiers = auditRetentionService.fetchAll()
                                                         .stream()
                                                         .map(AuditRetention::getAccountIdentifier)
                                                         .collect(Collectors.toSet());

      accountIdentifiers.removeAll(alreadyPresentAccountIdentifiers);
      accountIdentifiers.forEach(accountIdentifier -> auditRetentionService.create(accountIdentifier, 24));
    } catch (Exception e) {
      log.error("Error in Account and audit retention sync job", e);
    }
  }
}
