package io.harness.delegate.task.executioncapability;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.executeCommand;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.configuration.InstallUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChartMuseumCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    ChartMuseumCapability capability = (ChartMuseumCapability) delegateCapability;
    String chartMuseumPath = InstallUtils.getChartMuseumPath();
    if (isBlank(chartMuseumPath)) {
      return CapabilityResponse.builder().delegateCapability(capability).validated(false).build();
    }
    String chartMuseumVersionCommand =
        "${CHART_MUSEUM_PATH} -v".replace("${CHART_MUSEUM_PATH}", encloseWithQuotesIfNeeded(chartMuseumPath));
    return CapabilityResponse.builder()
        .validated(executeCommand(chartMuseumVersionCommand, 2))
        .delegateCapability(capability)
        .build();
  }
}
