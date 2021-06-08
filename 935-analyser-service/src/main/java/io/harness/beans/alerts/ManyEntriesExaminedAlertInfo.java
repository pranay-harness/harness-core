package io.harness.beans.alerts;

import io.harness.event.ExecutionStats;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManyEntriesExaminedAlertInfo implements AlertInfo {
  ExecutionStats executionStats;
}
