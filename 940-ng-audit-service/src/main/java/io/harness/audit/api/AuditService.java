package io.harness.audit.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.ng.beans.PageRequest;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface AuditService {
  Boolean create(AuditEventDTO auditEventDTO);

  Page<AuditEvent> list(
      String accountIdentifier, PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO);

  void deleteExpiredAudits(String accountIdentifier, Instant toBeDeletedTillTimestamp);

  List<String> fetchDistinctAccounts();
}
