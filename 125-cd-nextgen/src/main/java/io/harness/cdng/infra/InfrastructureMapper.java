/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.infra;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.common.ParameterFieldHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.environment.EnvironmentOutcome;

import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@OwnedBy(HarnessTeam.CDP)
public class InfrastructureMapper {
  public InfrastructureOutcome toOutcome(
      @Nonnull Infrastructure infrastructure, EnvironmentOutcome environmentOutcome, ServiceStepOutcome service) {
    switch (infrastructure.getKind()) {
      case InfrastructureKind.KUBERNETES_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        validateK8sDirectInfrastructure(k8SDirectInfrastructure);
        return K8sDirectInfrastructureOutcome.builder()
            .connectorRef(k8SDirectInfrastructure.getConnectorRef().getValue())
            .namespace(k8SDirectInfrastructure.getNamespace().getValue())
            .releaseName(getValueOrExpression(k8SDirectInfrastructure.getReleaseName()))
            .environment(environmentOutcome)
            .infrastructureKey(InfrastructureKey.generate(
                service, environmentOutcome, k8SDirectInfrastructure.getInfrastructureKeyValues()))
            .build();

      case InfrastructureKind.KUBERNETES_GCP:
        K8sGcpInfrastructure k8sGcpInfrastructure = (K8sGcpInfrastructure) infrastructure;
        validateK8sGcpInfrastructure(k8sGcpInfrastructure);
        return K8sGcpInfrastructureOutcome.builder()
            .connectorRef(k8sGcpInfrastructure.getConnectorRef().getValue())
            .namespace(k8sGcpInfrastructure.getNamespace().getValue())
            .cluster(k8sGcpInfrastructure.getCluster().getValue())
            .releaseName(getValueOrExpression(k8sGcpInfrastructure.getReleaseName()))
            .environment(environmentOutcome)
            .infrastructureKey(InfrastructureKey.generate(
                service, environmentOutcome, k8sGcpInfrastructure.getInfrastructureKeyValues()))
            .build();

      default:
        throw new InvalidArgumentsException(format("Unknown Infrastructure Kind : [%s]", infrastructure.getKind()));
    }
  }

  private void validateK8sDirectInfrastructure(K8SDirectInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getNamespace())
        || isEmpty(ParameterFieldHelper.getParameterFieldValue(infrastructure.getNamespace()))) {
      throw new InvalidArgumentsException(Pair.of("namespace", "cannot be empty"));
    }

    if (!hasValueOrExpression(infrastructure.getReleaseName())) {
      throw new InvalidArgumentsException(Pair.of("releaseName", "cannot be empty"));
    }
  }

  private void validateK8sGcpInfrastructure(K8sGcpInfrastructure infrastructure) {
    if (ParameterField.isNull(infrastructure.getNamespace())
        || isEmpty(ParameterFieldHelper.getParameterFieldValue(infrastructure.getNamespace()))) {
      throw new InvalidArgumentsException(Pair.of("namespace", "cannot be empty"));
    }

    if (!hasValueOrExpression(infrastructure.getReleaseName())) {
      throw new InvalidArgumentsException(Pair.of("releaseName", "cannot be empty"));
    }

    if (ParameterField.isNull(infrastructure.getCluster())
        || isEmpty(ParameterFieldHelper.getParameterFieldValue(infrastructure.getCluster()))) {
      throw new InvalidArgumentsException(Pair.of("cluster", "cannot be empty"));
    }
  }

  private boolean hasValueOrExpression(ParameterField<String> parameterField) {
    if (ParameterField.isNull(parameterField)) {
      return false;
    }

    return parameterField.isExpression() || !isEmpty(ParameterFieldHelper.getParameterFieldValue(parameterField));
  }

  private String getValueOrExpression(ParameterField<String> parameterField) {
    if (parameterField.isExpression()) {
      return parameterField.getExpressionValue();
    } else {
      return parameterField.getValue();
    }
  }
}
