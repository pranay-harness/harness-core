package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "KubernetesAuth", description = "This contains kubernetes auth details")
public class KubernetesAuthDTO {
  @NotNull @JsonProperty("type") KubernetesAuthType authType;

  @Builder
  public KubernetesAuthDTO(KubernetesAuthType authType, KubernetesAuthCredentialDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  KubernetesAuthCredentialDTO credentials;
}
