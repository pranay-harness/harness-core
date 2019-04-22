package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProcessExecutorCapability implements ExecutionCapability {
  private List<String> processExecutorArguments;
  private String category;
  @Default private final CapabilityType capabilityType = CapabilityType.PROCESS_EXECUTOR;

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    return category;
  }
}
