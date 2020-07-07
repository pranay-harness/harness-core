package io.harness.cdng.infra.yaml;

import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
public class K8SDirectInfrastructure implements Infrastructure {
  @Wither String connectorId;
  @Wither String namespace;
  @Wither String releaseName;

  @Override
  public InfraMapping getInfraMapping() {
    return K8sDirectInfraMapping.builder().k8sConnector(connectorId).namespace(namespace).build();
  }

  @Override
  public InfrastructureKind getKind() {
    return InfrastructureKind.K8S_DIRECT;
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    K8SDirectInfrastructure config = (K8SDirectInfrastructure) overrideConfig;
    K8SDirectInfrastructure resultantInfra = this;
    if (EmptyPredicate.isNotEmpty(config.getConnectorId())) {
      resultantInfra = resultantInfra.withConnectorId(config.getConnectorId());
    }
    if (EmptyPredicate.isNotEmpty(config.getNamespace())) {
      resultantInfra = resultantInfra.withNamespace(config.getNamespace());
    }
    if (EmptyPredicate.isNotEmpty(config.getReleaseName())) {
      resultantInfra = resultantInfra.withReleaseName(config.getReleaseName());
    }
    return resultantInfra;
  }
}
