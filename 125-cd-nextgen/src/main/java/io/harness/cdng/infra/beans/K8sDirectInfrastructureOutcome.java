package io.harness.cdng.infra.beans;

import io.harness.cdng.infra.yaml.InfrastructureKind;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sDirectInfrastructureOutcome implements InfrastructureOutcome {
  String connectorIdentifier;
  String namespace;
  String releaseName;

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_DIRECT;
  }
}
