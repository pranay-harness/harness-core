package io.harness.cvng.statemachine.entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Data
@Builder
@NoArgsConstructor
@Slf4j
public class TestTimeSeriesAnalysisState extends TimeSeriesAnalysisState {
  private final StateType type = StateType.TEST_TIME_SERIES;
}
