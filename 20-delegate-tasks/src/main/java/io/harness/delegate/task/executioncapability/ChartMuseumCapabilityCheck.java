package io.harness.delegate.task.executioncapability;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.configuration.InstallUtils;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
public class ChartMuseumCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    ChartMuseumCapability chartMuseumCapability = (ChartMuseumCapability) delegateCapability;
    String chartMuseumVersionCommand = chartMuseumCapability.getChartMuseumCommand().replace(
        "${CHART_MUSEUM_PATH}", encloseWithQuotesIfNeeded(InstallUtils.getChartMuseumPath()));

    ProcessExecutor processExecutor = new ProcessExecutor().command("/bin/sh", "-c", chartMuseumVersionCommand);
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      String msg = "[Delegate Capability] Failed to execute command with arguments, " + chartMuseumVersionCommand;
      logger.error(msg);
    }

    return CapabilityResponse.builder().delegateCapability(chartMuseumCapability).validated(valid).build();
  }
}
