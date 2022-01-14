package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisState;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class TimeSeriesAnalysisState extends AnalysisState {
  private String workerTaskId;
}
