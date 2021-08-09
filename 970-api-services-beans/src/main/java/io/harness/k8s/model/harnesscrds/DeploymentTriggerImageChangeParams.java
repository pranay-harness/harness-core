package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DeploymentTriggerImageChangeParams {
  private Boolean automatic;
  private List<String> containerNames = new ArrayList();
  private ObjectReference from;
  private String lastTriggeredImage;
}
