package io.harness.audit.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.AuditEvent;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface AuditRepository extends PagingAndSortingRepository<AuditEvent, String>, AuditRepositoryCustom {}
