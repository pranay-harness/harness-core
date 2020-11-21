package software.wings.api;

import software.wings.sm.StepExecutionSummary;

import lombok.Getter;
import lombok.Setter;

public class BarrierStepExecutionSummary extends StepExecutionSummary {
  @Getter @Setter private String identifier;
}
