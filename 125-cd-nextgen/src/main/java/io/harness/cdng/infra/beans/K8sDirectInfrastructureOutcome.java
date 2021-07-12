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
@JsonTypeName(InfrastructureKind.KUBERNETES_DIRECT)
@TypeAlias("cdng.infra.beans.K8sDirectInfrastructureOutcome")
@OwnedBy(HarnessTeam.CDP)
public class K8sDirectInfrastructureOutcome implements InfrastructureOutcome {
  String connectorRef;
  String namespace;
  String releaseName;
  EnvironmentOutcome environment;
  String infrastructureKey;

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_DIRECT;
  }
}
