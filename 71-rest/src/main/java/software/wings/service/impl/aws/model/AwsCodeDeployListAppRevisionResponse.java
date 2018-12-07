package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListAppRevisionResponse extends AwsResponse {
  private AwsCodeDeployS3LocationData s3LocationData;

  @Builder
  public AwsCodeDeployListAppRevisionResponse(
      ExecutionStatus executionStatus, String errorMessage, AwsCodeDeployS3LocationData s3LocationData) {
    super(executionStatus, errorMessage);
    this.s3LocationData = s3LocationData;
  }
}