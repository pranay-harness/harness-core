package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentTriggerPolicy {
  private DeploymentTriggerImageChangeParams imageChangeParams;
  private String type;
}