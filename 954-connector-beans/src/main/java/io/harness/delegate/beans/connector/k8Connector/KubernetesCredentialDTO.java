package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = KubernetesCredentialDTODeserializer.class)
@Schema(name = "KubernetesCredential", description = "This contains kubernetes credentials details")
public class KubernetesCredentialDTO {
  @NotNull @JsonProperty("type") KubernetesCredentialType kubernetesCredentialType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  KubernetesCredentialSpecDTO config;

  @Builder
  public KubernetesCredentialDTO(
      KubernetesCredentialType kubernetesCredentialType, KubernetesCredentialSpecDTO config) {
    this.kubernetesCredentialType = kubernetesCredentialType;
    this.config = config;
  }
}
