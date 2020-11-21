package io.harness.cdng.infra.beans;

import io.harness.cdng.infra.yaml.InfrastructureKind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(InfrastructureKind.KUBERNETES_DIRECT)
public class K8sDirectInfrastructureOutcome implements InfrastructureOutcome {
  String connectorRef;
  String namespace;
  String releaseName;

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_DIRECT;
  }
}
