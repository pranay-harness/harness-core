package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 2/6/17.
 */
@OwnedBy(CDC)
@JsonTypeName("phaseExecutionSummary")
public class PhaseExecutionSummary extends StepExecutionSummary implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "phaseExecutionSummary";

  private Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap = new HashMap<>();

  public Map<String, PhaseStepExecutionSummary> getPhaseStepExecutionSummaryMap() {
    return phaseStepExecutionSummaryMap;
  }

  public void setPhaseStepExecutionSummaryMap(Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap) {
    this.phaseStepExecutionSummaryMap = phaseStepExecutionSummaryMap;
  }

  @Override
  public String getType() {
    return "phaseExecutionSummary";
  }
}
