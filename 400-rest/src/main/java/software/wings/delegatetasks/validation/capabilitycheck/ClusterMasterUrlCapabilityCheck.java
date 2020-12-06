package software.wings.delegatetasks.validation.capabilitycheck;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.network.Http;

import software.wings.delegatetasks.validation.ContainerValidationHelper;
import software.wings.delegatetasks.validation.capabilities.ClusterMasterUrlValidationCapability;

import com.google.inject.Inject;

@TargetModule(Module._930_DELEGATE_TASKS)
public class ClusterMasterUrlCapabilityCheck implements CapabilityCheck {
  @Inject ContainerValidationHelper containerValidationHelper;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    ClusterMasterUrlValidationCapability capability = (ClusterMasterUrlValidationCapability) delegateCapability;
    String masterUrl = containerValidationHelper.getK8sMasterUrl(capability.getContainerServiceParams());
    boolean valid = Http.connectableHttpUrl(masterUrl);
    return CapabilityResponse.builder().delegateCapability(capability).validated(valid).build();
  }
}
