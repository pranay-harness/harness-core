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
public class SLIMetricAnalysisState extends AnalysisState {
  @Override
  public StateType getType() {
    return StateType.SLI_METRIC_ANALYSIS;
  }
}
