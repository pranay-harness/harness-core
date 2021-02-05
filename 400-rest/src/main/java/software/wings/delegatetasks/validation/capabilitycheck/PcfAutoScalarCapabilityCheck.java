package software.wings.delegatetasks.validation.capabilitycheck;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.pcf.PcfUtils;

import software.wings.delegatetasks.validation.capabilities.PcfAutoScalarCapability;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfAutoScalarCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfAutoScalarCapability pcfAutoScalarCapability = (PcfAutoScalarCapability) delegateCapability;
    try {
      boolean validated = PcfUtils.checkIfAppAutoscalarInstalled();
      if (!validated) {
        log.warn(
            "Could not find App Autoscalar plugin installed. CF PLUGIN HOME Used: {}", PcfUtils.resolvePcfPluginHome());
      }
      return CapabilityResponse.builder().delegateCapability(pcfAutoScalarCapability).validated(validated).build();
    } catch (Exception e) {
      log.error("Failed to Validate App-Autoscalar Plugin installed");
      return CapabilityResponse.builder().delegateCapability(pcfAutoScalarCapability).validated(false).build();
    }
  }
}
