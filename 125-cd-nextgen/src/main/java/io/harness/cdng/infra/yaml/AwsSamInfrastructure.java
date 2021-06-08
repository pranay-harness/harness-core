package io.harness.cdng.infra.yaml;

import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfrastructureOutcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(InfrastructureKind.AWS_SAM_INFRA)
public class AwsSamInfrastructure implements Infrastructure, InfrastructureOutcome {
  private String connectorRef;

  @Override
  public InfraMapping getInfraMapping() {
    return null;
  }

  @Override
  public String getKind() {
    return InfrastructureKind.AWS_SAM_INFRA;
  }

  @Override
  public EnvironmentOutcome getEnvironment() {
    return null;
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    return null;
  }
}
