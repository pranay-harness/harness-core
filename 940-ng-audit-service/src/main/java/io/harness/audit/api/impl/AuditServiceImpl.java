package io.harness.audit.api.impl;

import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.persistence.AuditRepository;

import com.google.inject.Inject;

public class AuditServiceImpl implements AuditService {
  @Inject private AuditRepository auditRepository;
  @Override
  public AuditEventDTO create(AuditEventDTO auditEventDTO) {
    return auditRepository.save(auditEventDTO);
  }
}
