package software.wings.delegatetasks.validation.capabilities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfAutoScalarCapability implements ExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.PCF_AUTO_SCALAR;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return "cf_appautoscalar";
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
