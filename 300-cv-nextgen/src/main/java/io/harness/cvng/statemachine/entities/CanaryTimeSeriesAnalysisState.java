package io.harness.cvng.statemachine.entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@Slf4j
public class CanaryTimeSeriesAnalysisState extends TimeSeriesAnalysisState {
  @Override
  public StateType getType() {
    return StateType.CANARY_TIME_SERIES;
  }
}
