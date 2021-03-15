package io.harness.audit.api;

import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.entities.AuditEvent;

public interface AuditService { AuditEvent create(AuditEventDTO auditEventDTO); }
