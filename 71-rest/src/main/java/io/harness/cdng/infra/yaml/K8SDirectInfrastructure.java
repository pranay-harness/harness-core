package io.harness.cdng.infra.yaml;

import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8SDirectInfrastructure implements Infrastructure {
  private String k8sConnector;
  private String namespace;
  private String releaseName;

  @Override
  public InfraMapping getInfraMapping() {
    return K8sDirectInfraMapping.builder().k8sConnector(k8sConnector).namespace(namespace).build();
  }
}
