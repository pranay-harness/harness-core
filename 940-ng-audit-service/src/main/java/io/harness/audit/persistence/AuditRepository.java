package io.harness.audit.persistence;

import io.harness.annotation.HarnessRepo;
import io.harness.audit.entities.AuditEvent;

import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@ValidateOnExecution
public interface AuditRepository extends PagingAndSortingRepository<AuditEvent, String> {}
