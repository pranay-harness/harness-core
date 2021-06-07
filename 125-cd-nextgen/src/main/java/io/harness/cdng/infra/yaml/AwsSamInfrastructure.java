package io.harness.cdng.infra.yaml;

import io.harness.cdng.infra.beans.InfraMapping;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsSamInfrastructure implements Infrastructure {
  private String awsConnectorRef;

  @Override
  public InfraMapping getInfraMapping() {
    return null;
  }

  @Override
  public String getKind() {
    return InfrastructureKind.AWS_SAM_INFRA;
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    return null;
  }
}
