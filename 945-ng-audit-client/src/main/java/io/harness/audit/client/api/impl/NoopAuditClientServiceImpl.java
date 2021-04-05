package io.harness.audit.client.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;

@OwnedBy(PL)
public class NoopAuditClientServiceImpl implements AuditClientService {
  public boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext) {
    return true;
  }
}
