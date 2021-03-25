package io.harness.audit.client.api.impl;

import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;

public class NoopAuditClientServiceImpl implements AuditClientService {
  public boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext) {
    return true;
  }
}
