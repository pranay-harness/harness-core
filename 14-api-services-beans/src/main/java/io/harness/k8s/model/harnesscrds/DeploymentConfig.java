package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentConfig {
  private String apiVersion;
  private String kind;
  private V1ObjectMeta metadata;
  private DeploymentConfigSpec spec;
}