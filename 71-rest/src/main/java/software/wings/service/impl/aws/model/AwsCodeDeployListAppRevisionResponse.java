package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCodeDeployListAppRevisionResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private AwsCodeDeployS3LocationData s3LocationData;
}