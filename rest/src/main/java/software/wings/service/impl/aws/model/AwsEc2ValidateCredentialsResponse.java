package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ValidateCredentialsResponse extends AwsResponse {
  private boolean valid;

  @Builder
  public AwsEc2ValidateCredentialsResponse(ExecutionStatus executionStatus, String errorMessage, boolean valid) {
    super(executionStatus, errorMessage);
    this.valid = valid;
  }
}
