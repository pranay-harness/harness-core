package io.harness.cvng.statemachine.entities;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class ServiceGuardLogAnalysisState extends LogAnalysisState {
  private final StateType type = StateType.SERVICE_GUARD_LOG_ANALYSIS;
}
