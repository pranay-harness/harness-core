package io.harness.cdng.infra.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.yaml.InfrastructureKind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(InfrastructureKind.KUBERNETES_GCP)
@TypeAlias("cdng.infra.beans.K8sGcpInfrastructureOutcome")
@OwnedBy(HarnessTeam.CDP)
public class K8sGcpInfrastructureOutcome implements InfrastructureOutcome {
  String connectorRef;
  String namespace;
  String cluster;
  String releaseName;
  EnvironmentOutcome environment;

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_GCP;
  }
}
