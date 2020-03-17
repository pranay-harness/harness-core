package software.wings.delegatetasks.validation.capabilitycheck;

import com.google.inject.Inject;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.validation.capabilities.PcfAutoScalarCapability;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;

@Slf4j
public class PcfAutoScalarCapabilityCheck implements CapabilityCheck {
  @Inject private PcfDeploymentManager pcfDeploymentManager;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfAutoScalarCapability pcfAutoScalarCapability = (PcfAutoScalarCapability) delegateCapability;
    try {
      boolean validated = pcfDeploymentManager.checkIfAppAutoscalarInstalled();
      if (!validated) {
        logger.warn("Could not find App Autoscalar plugin installed. CF PLUGIN HOME Used: {}",
            pcfDeploymentManager.resolvePcfPluginHome());
      }
      return CapabilityResponse.builder().delegateCapability(pcfAutoScalarCapability).validated(validated).build();
    } catch (Exception e) {
      logger.error("Failed to Validate App-Autoscalar Plugin installed");
      return CapabilityResponse.builder().delegateCapability(pcfAutoScalarCapability).validated(false).build();
    }
  }
}
