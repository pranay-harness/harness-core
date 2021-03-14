package io.harness.audit.persistence;

import io.harness.annotation.HarnessRepo;
import io.harness.audit.beans.AuditEventDTO;

import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@ValidateOnExecution
public interface AuditRepository extends PagingAndSortingRepository<AuditEventDTO, String> {}
