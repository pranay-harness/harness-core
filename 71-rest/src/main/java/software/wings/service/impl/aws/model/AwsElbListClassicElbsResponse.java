package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListClassicElbsResponse extends AwsResponse {
  private List<String> classicElbs;

  @Builder
  public AwsElbListClassicElbsResponse(ExecutionStatus executionStatus, String errorMessage, List<String> classicElbs) {
    super(executionStatus, errorMessage);
    this.classicElbs = classicElbs;
  }
}
