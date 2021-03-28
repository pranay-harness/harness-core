package io.harness.audit.client.api;

import io.harness.audit.beans.AuditEntry;
import io.harness.context.GlobalContext;

public interface AuditClientService {
  boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext);
}
