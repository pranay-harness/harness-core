/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.orchestrationEventLog;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.OrchestrationEventLog;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface OrchestrationEventLogRepositoryCustom {
  List<OrchestrationEventLog> findUnprocessedEvents(String planExecutionId, long lastUpdatedAt);

  void updateTtlForProcessedEvents(List<OrchestrationEventLog> eventLogs);

  void schemaMigrationForOldEvenLog();
}
