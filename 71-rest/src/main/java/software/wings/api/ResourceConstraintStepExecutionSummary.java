package software.wings.api;

import lombok.Getter;
import lombok.Setter;
import software.wings.sm.StepExecutionSummary;

public class ResourceConstraintStepExecutionSummary extends StepExecutionSummary {
  @Getter @Setter private String resourceConstraintName;
  @Getter @Setter private int resourceConstraintCapacity;
  @Getter @Setter private int usage;
}
