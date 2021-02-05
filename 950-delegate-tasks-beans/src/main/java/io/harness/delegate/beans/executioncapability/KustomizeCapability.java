package io.harness.delegate.beans.executioncapability;

import static io.harness.delegate.beans.executioncapability.CapabilityType.KUSTOMIZE;

import io.harness.data.structure.HarnessStringUtils;

import java.time.Duration;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KustomizeCapability implements ExecutionCapability {
  private String pluginRootDir;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public CapabilityType getCapabilityType() {
    return KUSTOMIZE;
  }

  @Override
  public String fetchCapabilityBasis() {
    return HarnessStringUtils.join(":", "kustomizePluginDir", pluginRootDir);
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
