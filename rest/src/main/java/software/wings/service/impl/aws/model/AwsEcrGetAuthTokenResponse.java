package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

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