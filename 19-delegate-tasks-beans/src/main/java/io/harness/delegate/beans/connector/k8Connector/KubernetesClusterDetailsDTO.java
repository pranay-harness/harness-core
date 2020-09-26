package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ManualConfig")
public class KubernetesClusterDetailsDTO implements KubernetesCredentialSpecDTO {
  @NotNull String masterUrl;
  @JsonProperty("auth") @NotNull @Valid KubernetesAuthDTO auth;
}
