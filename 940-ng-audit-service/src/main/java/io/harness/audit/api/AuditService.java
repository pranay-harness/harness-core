/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.ng.beans.PageRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface AuditService {
  Boolean create(AuditEventDTO auditEventDTO);

  Optional<AuditEvent> get(String accountIdentifier, String auditId);

  Page<AuditEvent> list(
      String accountIdentifier, PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO);

  void purgeAuditsOlderThanTimestamp(String accountIdentifier, Instant timestamp);

  Set<String> getUniqueAuditedAccounts();
}
