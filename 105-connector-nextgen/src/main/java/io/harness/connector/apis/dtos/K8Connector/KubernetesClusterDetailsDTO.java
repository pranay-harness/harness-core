package io.harness.connector.apis.dtos.K8Connector;

import com.google.gson.annotations.SerializedName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterDetailsDTO implements KubernetesCredentialDTO {
  String masterUrl;
  @SerializedName("kind") KubernetesAuthType authType;
  @SerializedName("spec") KubernetesAuthDTO auth;
}
