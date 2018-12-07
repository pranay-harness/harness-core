package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListSGsResponse extends AwsResponse {
  private List<String> securityGroups;

  @Builder
  public AwsEc2ListSGsResponse(ExecutionStatus executionStatus, String errorMessage, List<String> securityGroups) {
    super(executionStatus, errorMessage);
    this.securityGroups = securityGroups;
  }
}