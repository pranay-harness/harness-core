package io.harness.delegate.beans.executioncapability;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
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
}
