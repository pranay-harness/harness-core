package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsEc2ValidateCredentialsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private boolean valid;
}
