package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
