package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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