package io.harness.cdng.infra.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@JsonTypeName(InfrastructureKind.KUBERNETES_DIRECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8SDirectInfrastructure implements Infrastructure {
  @Wither String connectorIdentifier;
  @Wither String namespace;
  @Wither String releaseName;

  @Override
  public InfraMapping getInfraMapping() {
    return K8sDirectInfraMapping.builder().k8sConnector(connectorIdentifier).namespace(namespace).build();
  }

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_DIRECT;
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    K8SDirectInfrastructure config = (K8SDirectInfrastructure) overrideConfig;
    K8SDirectInfrastructure resultantInfra = this;
    if (EmptyPredicate.isNotEmpty(config.getConnectorIdentifier())) {
      resultantInfra = resultantInfra.withConnectorIdentifier(config.getConnectorIdentifier());
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
