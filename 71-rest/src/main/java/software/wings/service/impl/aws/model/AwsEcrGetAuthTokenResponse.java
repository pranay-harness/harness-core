package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcrGetAuthTokenResponse extends AwsResponse {
  String ecrAuthToken;

  @Builder
  public AwsEcrGetAuthTokenResponse(ExecutionStatus executionStatus, String errorMessage, String ecrAuthToken) {
    super(executionStatus, errorMessage);
    this.ecrAuthToken = ecrAuthToken;
  }
}