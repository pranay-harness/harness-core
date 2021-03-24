package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.ng.beans.PageRequest;

import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface AuditService {
  Boolean create(AuditEventDTO auditEventDTO);

  Page<AuditEvent> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO);
}
