package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 2/6/17.
 */
@OwnedBy(CDC)
public class PhaseExecutionSummary extends StepExecutionSummary implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "phaseExecutionSummary";

  private Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap = new HashMap<>();

  public Map<String, PhaseStepExecutionSummary> getPhaseStepExecutionSummaryMap() {
    return phaseStepExecutionSummaryMap;
  }

  public void setPhaseStepExecutionSummaryMap(Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap) {
    this.phaseStepExecutionSummaryMap = phaseStepExecutionSummaryMap;
  }
}
