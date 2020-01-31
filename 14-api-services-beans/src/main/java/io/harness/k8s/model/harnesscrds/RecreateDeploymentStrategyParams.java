package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RecreateDeploymentStrategyParams {
  private LifecycleHook mid;
  private LifecycleHook post;
  private LifecycleHook pre;
  private Long timeoutSeconds;
}