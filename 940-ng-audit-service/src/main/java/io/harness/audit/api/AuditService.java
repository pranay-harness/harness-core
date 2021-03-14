package io.harness.audit.api;

import io.harness.audit.beans.AuditEventDTO;

public interface AuditService { AuditEventDTO create(AuditEventDTO auditEventDTO); }
