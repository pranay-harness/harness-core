package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PcfConnectivityCapability implements ExecutionCapability {
  @NotNull private String endpointUrl;

  private final CapabilityType capabilityType = CapabilityType.PCF_CONNECTIVITY;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return "Pcf:" + endpointUrl;
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
