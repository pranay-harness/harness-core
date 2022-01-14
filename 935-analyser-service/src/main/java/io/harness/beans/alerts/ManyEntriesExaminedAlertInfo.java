package io.harness.beans.alerts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.ExecutionStats;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class ManyEntriesExaminedAlertInfo implements AlertInfo {
  ExecutionStats executionStats;
}
