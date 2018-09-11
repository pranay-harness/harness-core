package software.wings.sm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 2/6/17.
 */
public class PhaseStepExecutionSummary extends StepExecutionSummary {
  private List<StepExecutionSummary> stepExecutionSummaryList = new ArrayList<>();

  public List<StepExecutionSummary> getStepExecutionSummaryList() {
    return stepExecutionSummaryList;
  }

  public void setStepExecutionSummaryList(List<StepExecutionSummary> stepExecutionSummaryList) {
    this.stepExecutionSummaryList = stepExecutionSummaryList;
  }
}
