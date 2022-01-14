package io.harness.beans.alerts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.query.SortPattern;
import io.harness.event.QueryPlanner;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class SortStageAlertInfo implements AlertInfo {
  QueryPlanner queryPlanner;
  SortPattern sortPattern;
}
