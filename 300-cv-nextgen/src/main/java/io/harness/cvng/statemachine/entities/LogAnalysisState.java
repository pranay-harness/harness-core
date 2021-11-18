package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisState;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class LogAnalysisState extends AnalysisState {
  protected String workerTaskId;
}
