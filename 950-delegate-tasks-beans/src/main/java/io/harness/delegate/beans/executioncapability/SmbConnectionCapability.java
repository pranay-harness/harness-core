package io.harness.delegate.beans.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SmbConnectionCapability implements ExecutionCapability {
  @NonNull String smbUrl;
  @Builder.Default CapabilityType capabilityType = CapabilityType.SMB;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return smbUrl;
  }
}
