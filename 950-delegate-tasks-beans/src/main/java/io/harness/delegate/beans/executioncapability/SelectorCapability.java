package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.time.Duration;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class SelectorCapability implements ExecutionCapability {
  private Set<String> selectors;
  private String selectorOrigin;

  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SELECTORS;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.MANAGER;
  }

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return String.join(", ", selectors);
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return null;
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return null;
  }
}
