package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ManualConfig")
public class KubernetesClusterDetailsDTO implements KubernetesCredentialDTO {
  String masterUrl;
  @JsonProperty("auth") KubernetesAuthDTO auth;
}
