/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.orchestrationEventLog;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.OrchestrationEventLog;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface OrchestrationEventLogRepository
    extends PagingAndSortingRepository<OrchestrationEventLog, String>, OrchestrationEventLogRepositoryCustom {}
