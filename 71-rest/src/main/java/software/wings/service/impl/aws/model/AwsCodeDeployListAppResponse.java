package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCodeDeployListAppResponse extends AwsResponse {
  private List<String> applications;

  @Builder
  public AwsCodeDeployListAppResponse(ExecutionStatus executionStatus, String errorMessage, List<String> applications) {
    super(executionStatus, errorMessage);
    this.applications = applications;
  }
}
