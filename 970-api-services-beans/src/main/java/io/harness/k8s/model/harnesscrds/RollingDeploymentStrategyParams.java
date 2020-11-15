package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.custom.IntOrString;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RollingDeploymentStrategyParams {
  private Long intervalSeconds;
  private IntOrString maxSurge;
  private IntOrString maxUnavailable;
  private LifecycleHook post;
  private LifecycleHook pre;
  private Long timeoutSeconds;
  private Long updatePeriodSeconds;
}