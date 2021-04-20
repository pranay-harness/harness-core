package io.harness.audit.client.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.context.GlobalContext;

@OwnedBy(PL)
public interface AuditClientService {
  boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext);
}
