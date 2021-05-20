package io.harness.engine.events;

import io.harness.pms.sdk.core.events.OrchestrationEventLog;

public interface OrchestrationEventLogHandler {
  void handleLog(OrchestrationEventLog eventLog);
}
