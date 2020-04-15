package software.wings.delegatetasks.validation.capabilitycheck;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.network.Http;
import software.wings.delegatetasks.validation.ContainerValidationHelper;
import software.wings.delegatetasks.validation.capabilities.ClusterMasterUrlValidationCapability;

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
