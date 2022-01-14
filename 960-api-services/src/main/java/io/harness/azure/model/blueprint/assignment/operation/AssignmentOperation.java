package io.harness.azure.model.blueprint.assignment.operation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentOperation {
  private String id;
  private String name;
  private String type;
  private Properties properties;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Properties {
    private String assignmentState;
    private String blueprintVersion;
    private String timeCreated;
    private String timeFinished;
    private String timeStarted;
    private AssignmentDeploymentJob[] deployments;
  }
}
