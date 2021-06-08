package io.harness.beans.alerts;

import io.harness.beans.query.SortPattern;
import io.harness.event.QueryPlanner;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SortStageAlertInfo implements AlertInfo {
  QueryPlanner queryPlanner;
  SortPattern sortPattern;
}
