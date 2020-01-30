package io.harness.delegate.task.executioncapability;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HelmCapability;
import io.harness.delegate.configuration.InstallUtils;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
public class HelmCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HelmCapability helmCapability = (HelmCapability) delegateCapability;
    String helmVersionCommand =
        helmCapability.getHelmCommand().replace("${HELM_PATH}", encloseWithQuotesIfNeeded(InstallUtils.getHelm2Path()));

    ProcessExecutor processExecutor = new ProcessExecutor().command("/bin/sh", "-c", helmVersionCommand);
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
      logger.info("[Delegate Capability] Successfully executed command with arguments" + helmVersionCommand);
    } catch (Exception e) {
      String msg = "[Delegate Capability] Failed to execute command with arguments, " + helmVersionCommand;
      logger.error(msg);
    }

    return CapabilityResponse.builder().delegateCapability(helmCapability).validated(valid).build();
  }
}
