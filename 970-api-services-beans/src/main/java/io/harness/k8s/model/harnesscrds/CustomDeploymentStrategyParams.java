package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CustomDeploymentStrategyParams {
  private List<String> command = new ArrayList();
  private List<V1EnvVar> environment = new ArrayList();
  private String image;
}