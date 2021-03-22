package io.harness.cdng.infra;

import static java.lang.String.format;

import io.harness.steps.shellScript.environment.EnvironmentOutcome;
import io.harness.steps.shellScript.beans.InfrastructureOutcome;
import io.harness.steps.shellScript.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.steps.shellScript.yaml.InfrastructureKind;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.exception.InvalidArgumentsException;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
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
      default:
        throw new InvalidArgumentsException(format("Unknown Infrastructure Kind : [%s]", infrastructure.getKind()));
    }
  }
}
