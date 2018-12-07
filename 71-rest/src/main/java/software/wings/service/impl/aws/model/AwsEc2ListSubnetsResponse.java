package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListSubnetsResponse extends AwsResponse {
  private List<String> subnets;

  @Builder
  public AwsEc2ListSubnetsResponse(ExecutionStatus executionStatus, String errorMessage, List<String> subnets) {
    super(executionStatus, errorMessage);
    this.subnets = subnets;
  }
}