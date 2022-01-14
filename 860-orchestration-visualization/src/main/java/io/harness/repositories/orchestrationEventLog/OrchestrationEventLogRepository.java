package io.harness.repositories.orchestrationEventLog;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.OrchestrationEventLog;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface OrchestrationEventLogRepository
    extends PagingAndSortingRepository<OrchestrationEventLog, String>, OrchestrationEventLogRepositoryCustom {}
