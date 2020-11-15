package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentTriggerImageChangeParams {
  private Boolean automatic;
  private List<String> containerNames = new ArrayList();
  private ObjectReference from;
  private String lastTriggeredImage;
}