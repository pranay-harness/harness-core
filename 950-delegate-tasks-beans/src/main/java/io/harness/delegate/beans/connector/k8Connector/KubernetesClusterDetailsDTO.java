package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(KubernetesConfigConstants.MANUAL_CREDENTIALS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterDetailsDTO implements KubernetesCredentialSpecDTO {
  @NotNull String masterUrl;
  @JsonProperty("auth") @NotNull @Valid KubernetesAuthDTO auth;
}
