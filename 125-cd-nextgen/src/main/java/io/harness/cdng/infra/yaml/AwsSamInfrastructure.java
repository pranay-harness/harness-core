package io.harness.cdng.infra.yaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.infra.beans.InfraMapping;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(InfrastructureKind.AWS_SAM_INFRA)
public class AwsSamInfrastructure implements Infrastructure {
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
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    return null;
  }
}
