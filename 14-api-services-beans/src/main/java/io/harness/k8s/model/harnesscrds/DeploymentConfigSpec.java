package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentConfigSpec {
  private Integer minReadySeconds;
  private Boolean paused;
  private Integer replicas;
  private Integer revisionHistoryLimit;
  private Map<String, String> selector;
  private DeploymentStrategy strategy;
  private V1PodTemplateSpec template;
  private Boolean test;
  private List<DeploymentTriggerPolicy> triggers = new ArrayList();
}