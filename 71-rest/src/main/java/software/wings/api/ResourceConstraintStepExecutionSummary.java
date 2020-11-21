package software.wings.api;

import software.wings.sm.StepExecutionSummary;

import lombok.Getter;
import lombok.Setter;

public class ResourceConstraintStepExecutionSummary extends StepExecutionSummary {
  @Getter @Setter private String resourceConstraintName;
  @Getter @Setter private int resourceConstraintCapacity;
  @Getter @Setter private String unit;
  @Getter @Setter private int usage;
  @Getter @Setter private int alreadyAcquiredPermits;
}
