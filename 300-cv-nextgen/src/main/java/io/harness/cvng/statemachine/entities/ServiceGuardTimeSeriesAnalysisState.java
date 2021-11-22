package io.harness.cvng.statemachine.entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@Slf4j
public class ServiceGuardTimeSeriesAnalysisState extends TimeSeriesAnalysisState {
  private final StateType type = StateType.SERVICE_GUARD_TIME_SERIES;
}
