package io.harness.cvng.statemachine.entities;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class ServiceGuardLogAnalysisState extends LogAnalysisState {
  @Override
  protected String scheduleAnalysis(AnalysisInput analysisInput) {
    return getLogAnalysisService().scheduleServiceGuardLogAnalysisTask(analysisInput);
  }
}
