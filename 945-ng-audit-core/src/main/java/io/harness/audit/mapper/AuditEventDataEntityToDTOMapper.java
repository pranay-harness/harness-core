package io.harness.audit.mapper;

import io.harness.audit.AuditEventData;
import io.harness.audit.beans.AuditEventDataDTO;

public interface AuditEventDataEntityToDTOMapper<D extends AuditEventDataDTO, B extends AuditEventData> {
  D toDTO(B auditEventData);
}
