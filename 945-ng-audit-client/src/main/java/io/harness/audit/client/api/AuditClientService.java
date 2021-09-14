/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.client.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuthenticationInfoDTO;
import io.harness.context.GlobalContext;

@OwnedBy(PL)
public interface AuditClientService {
  boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext);
  boolean publishAudit(AuditEntry auditEntry, AuthenticationInfoDTO authenticationInfo, GlobalContext globalContext);
}
