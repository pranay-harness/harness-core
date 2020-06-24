package io.harness.connector.apis.dtos.K8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterDetailsDTO implements KubernetesCredentialDTO {
  String masterUrl;
  @JsonProperty("auth") KubernetesAuthDTO auth;
}
