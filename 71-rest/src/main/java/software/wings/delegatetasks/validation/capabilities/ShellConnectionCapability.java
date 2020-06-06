package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.delegation.ShellScriptParameters;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ShellConnectionCapability implements ExecutionCapability {
  @NotNull ShellScriptParameters shellScriptParameters;
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SHELL_CONNECTION;

  @Override
  public String fetchCapabilityBasis() {
    return shellScriptParameters.getHost();
  }
}
