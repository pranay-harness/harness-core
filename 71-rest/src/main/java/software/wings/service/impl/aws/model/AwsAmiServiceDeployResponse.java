package software.wings.service.impl.aws.model;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiServiceDeployResponse extends AwsResponse {
  private List<Instance> instancesAdded;

  @Builder
  public AwsAmiServiceDeployResponse(
      ExecutionStatus executionStatus, String errorMessage, List<Instance> instancesAdded) {
    super(executionStatus, errorMessage);
    this.instancesAdded = instancesAdded;
  }
}