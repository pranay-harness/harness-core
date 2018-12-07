package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiSwitchRoutesResponse extends AwsResponse {
  @Builder
  public AwsAmiSwitchRoutesResponse(ExecutionStatus executionStatus, String errorMessage) {
    super(executionStatus, errorMessage);
  }
}