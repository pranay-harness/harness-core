package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import software.wings.beans.delegation.ShellScriptParameters;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShellConnectionCapability implements ExecutionCapability {
  @NotNull ShellScriptParameters shellScriptParameters;
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SHELL_CONNECTION;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return shellScriptParameters.getHost();
  }
}
