package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO extends ConnectorConfigDTO {
  @NotNull @JsonProperty("type") KubernetesCredentialType kubernetesCredentialType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  KubernetesCredentialDTO config;

  @Builder
  public KubernetesClusterConfigDTO(KubernetesCredentialType kubernetesCredentialType, KubernetesCredentialDTO config) {
    this.kubernetesCredentialType = kubernetesCredentialType;
    this.config = config;
  }
}