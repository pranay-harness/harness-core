package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("K8sCluster")
public class KubernetesClusterConfigDTO extends ConnectorConfigDTO {
  @JsonProperty("type") KubernetesCredentialType kubernetesCredentialType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  KubernetesCredentialDTO config;

  @Builder
  public KubernetesClusterConfigDTO(KubernetesCredentialType kubernetesCredentialType, KubernetesCredentialDTO config) {
    this.kubernetesCredentialType = kubernetesCredentialType;
    this.config = config;
  }
}