package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListDeploymentConfigResponse extends AwsResponse {
  private List<String> deploymentConfig;

  @Builder
  public AwsCodeDeployListDeploymentConfigResponse(
      ExecutionStatus executionStatus, String errorMessage, List<String> deploymentConfig) {
    super(executionStatus, errorMessage);
    this.deploymentConfig = deploymentConfig;
  }
}