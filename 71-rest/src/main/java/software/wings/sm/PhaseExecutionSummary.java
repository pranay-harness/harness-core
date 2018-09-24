package software.wings.sm;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 2/6/17.
 */
public class PhaseExecutionSummary extends StepExecutionSummary {
  private Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap = new HashMap<>();

  public Map<String, PhaseStepExecutionSummary> getPhaseStepExecutionSummaryMap() {
    return phaseStepExecutionSummaryMap;
  }

  public void setPhaseStepExecutionSummaryMap(Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap) {
    this.phaseStepExecutionSummaryMap = phaseStepExecutionSummaryMap;
  }
}
