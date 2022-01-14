package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChartMuseumCapability implements ExecutionCapability {
  CapabilityType capabilityType = CapabilityType.CHART_MUSEUM;
  boolean useLatestChartMuseumVersion;
  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return capabilityType.name();
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
