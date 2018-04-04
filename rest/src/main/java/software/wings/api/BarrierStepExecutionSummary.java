package software.wings.api;

import software.wings.sm.StepExecutionSummary;

public class BarrierStepExecutionSummary extends StepExecutionSummary {
  private String identifier;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
}
