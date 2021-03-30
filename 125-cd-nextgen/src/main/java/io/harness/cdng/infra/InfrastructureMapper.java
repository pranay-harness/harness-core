package io.harness.cdng.infra;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.exception.InvalidArgumentsException;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDP)
public class InfrastructureMapper {
  public InfrastructureOutcome toOutcome(
      @Nonnull Infrastructure infrastructure, EnvironmentOutcome environmentOutcome) {
    switch (infrastructure.getKind()) {
      case InfrastructureKind.KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        return K8sDirectInfrastructureOutcome.builder()
            .connectorRef(k8SDirectInfrastructure.getConnectorRef().getValue())
            .namespace(k8SDirectInfrastructure.getNamespace().getValue())
            .releaseName(k8SDirectInfrastructure.getReleaseName().getValue())
            .environment(environmentOutcome)
            .build();

      case InfrastructureKind.KUBERNETES_GCP:
        K8sGcpInfrastructure k8sGcpInfrastructure = (K8sGcpInfrastructure) infrastructure;
        return K8sGcpInfrastructureOutcome.builder()
            .connectorRef(k8sGcpInfrastructure.getConnectorRef().getValue())
            .namespace(k8sGcpInfrastructure.getNamespace().getValue())
            .cluster(k8sGcpInfrastructure.getCluster().getValue())
            .releaseName(k8sGcpInfrastructure.getReleaseName().getValue())
            .environment(environmentOutcome)
            .build();

      default:
        throw new InvalidArgumentsException(format("Unknown Infrastructure Kind : [%s]", infrastructure.getKind()));
    }
  }
}
