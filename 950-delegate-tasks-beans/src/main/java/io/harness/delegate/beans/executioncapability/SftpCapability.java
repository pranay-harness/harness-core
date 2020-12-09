package io.harness.delegate.beans.executioncapability;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class SftpCapability implements ExecutionCapability {
  @NotNull String sftpUrl;
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SFTP;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return sftpUrl;
  }
}
