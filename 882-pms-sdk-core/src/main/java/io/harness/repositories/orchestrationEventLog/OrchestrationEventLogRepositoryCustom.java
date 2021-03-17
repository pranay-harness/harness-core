package io.harness.repositories.orchestrationEventLog;

import io.harness.pms.sdk.core.events.OrchestrationEventLog;

import java.util.List;

public interface OrchestrationEventLogRepositoryCustom {
  List<OrchestrationEventLog> findUnprocessedEvents(String planExecutionId, long lastUpdatedAt);
  List<OrchestrationEventLog> findUnprocessedEvents(String planExecutionId);
}
