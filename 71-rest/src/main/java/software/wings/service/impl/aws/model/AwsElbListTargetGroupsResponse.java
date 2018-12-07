package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListTargetGroupsResponse extends AwsResponse {
  private Map<String, String> targetGroups;

  @Builder
  public AwsElbListTargetGroupsResponse(
      ExecutionStatus executionStatus, String errorMessage, Map<String, String> targetGroups) {
    super(executionStatus, errorMessage);
    this.targetGroups = targetGroups;
  }
}