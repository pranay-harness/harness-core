package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListRegionsResponse extends AwsResponse {
  private List<String> regions;

  @Builder
  public AwsEc2ListRegionsResponse(ExecutionStatus executionStatus, String errorMessage, List<String> regions) {
    super(executionStatus, errorMessage);
    this.regions = regions;
  }
}