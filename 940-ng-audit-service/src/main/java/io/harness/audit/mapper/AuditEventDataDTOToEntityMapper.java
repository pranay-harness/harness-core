package io.harness.audit.mapper;

import io.harness.audit.AuditEventData;
import io.harness.audit.beans.AuditEventDataDTO;

public interface AuditEventDataDTOToEntityMapper<B extends AuditEventData, D extends AuditEventDataDTO> {
  B fromDTO(D dto);
}
