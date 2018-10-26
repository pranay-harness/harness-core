package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAmiSwitchRoutesResponse extends AwsResponse {
  @Builder
  public AwsAmiSwitchRoutesResponse(ExecutionStatus executionStatus, String errorMessage) {
    super(executionStatus, errorMessage);
  }
}