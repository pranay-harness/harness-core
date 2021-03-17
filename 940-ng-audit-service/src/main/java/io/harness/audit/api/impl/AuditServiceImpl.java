package io.harness.audit.api.impl;

import static io.harness.audit.mapper.AuditEventMapper.fromDTO;

import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.persistence.AuditRepository;

import com.google.inject.Inject;

public class AuditServiceImpl implements AuditService {
  @Inject private AuditRepository auditRepository;
  @Override
  public AuditEvent create(AuditEventDTO auditEventDTO) {
    AuditEvent auditEvent = fromDTO(auditEventDTO);
    return auditRepository.save(auditEvent);
  }
}
