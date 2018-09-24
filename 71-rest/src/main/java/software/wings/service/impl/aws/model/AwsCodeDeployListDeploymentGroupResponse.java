package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListDeploymentGroupResponse extends AwsResponse {
  private List<String> deploymentGroups;

  @Builder
  public AwsCodeDeployListDeploymentGroupResponse(
      ExecutionStatus executionStatus, String errorMessage, List<String> deploymentGroups) {
    super(executionStatus, errorMessage);
    this.deploymentGroups = deploymentGroups;
  }
}