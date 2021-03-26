package io.harness.audit.retention;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.entities.AuditRetention;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AuditAccountSyncJob implements Runnable {
  @Inject private AuditService auditService;
  @Inject private AuditRetentionService auditRetentionService;

  @Override
  public void run() {
    try {
      List<String> accountIdentifiers = auditService.fetchDistinctAccounts();
      if (accountIdentifiers != null && accountIdentifiers.size() > 0) {
        for (String accountIdentifier : accountIdentifiers) {
          AuditRetention auditRetention = auditRetentionService.get(accountIdentifier);
          if (auditRetention == null) {
            auditRetentionService.create(accountIdentifier);
          }
        }
      }
    } catch (Exception e) {
      log.error("Error in Account and audit retention sync job", e);
    }
  }
}
