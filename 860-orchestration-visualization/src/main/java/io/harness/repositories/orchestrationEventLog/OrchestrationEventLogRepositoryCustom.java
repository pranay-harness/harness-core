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
  void deleteLogsForGivenPlanExecutionId(String planExecutionId);
}
