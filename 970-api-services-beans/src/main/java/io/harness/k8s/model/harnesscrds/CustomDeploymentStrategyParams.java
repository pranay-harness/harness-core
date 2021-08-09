package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.openapi.models.V1EnvVar;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CustomDeploymentStrategyParams {
  private List<String> command = new ArrayList();
  private List<V1EnvVar> environment = new ArrayList();
  private String image;
}
