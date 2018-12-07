package software.wings.service.impl.aws.model;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListDeploymentInstancesResponse extends AwsResponse {
  private List<Instance> instances;

  @Builder
  public AwsCodeDeployListDeploymentInstancesResponse(
      ExecutionStatus executionStatus, String errorMessage, List<Instance> instances) {
    super(executionStatus, errorMessage);
    this.instances = instances;
  }
}