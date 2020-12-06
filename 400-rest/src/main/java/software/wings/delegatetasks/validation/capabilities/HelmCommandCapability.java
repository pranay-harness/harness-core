package software.wings.delegatetasks.validation.capabilities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import software.wings.helpers.ext.helm.request.HelmCommandRequest;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._930_DELEGATE_TASKS)
public class HelmCommandCapability implements ExecutionCapability {
  @NotNull HelmCommandRequest commandRequest;
  CapabilityType capabilityType = CapabilityType.HELM_COMMAND;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return "Helm Installed. Version : " + commandRequest.getHelmVersion().name();
  }
}
