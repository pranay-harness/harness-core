package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListVpcsResponse extends AwsResponse {
  private List<String> vpcs;

  @Builder
  public AwsEc2ListVpcsResponse(ExecutionStatus executionStatus, String errorMessage, List<String> vpcs) {
    super(executionStatus, errorMessage);
    this.vpcs = vpcs;
  }
}