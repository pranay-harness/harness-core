package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisState;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ServiceGuardTrendAnalysisState extends AnalysisState {
  private String workerTaskId;

  private final StateType type = StateType.SERVICE_GUARD_TREND_ANALYSIS;
}
