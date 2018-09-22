package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsElbListAppElbsResponse extends AwsResponse {
  private List<String> appElbs;

  @Builder
  public AwsElbListAppElbsResponse(ExecutionStatus executionStatus, String errorMessage, List<String> appElbs) {
    super(executionStatus, errorMessage);
    this.appElbs = appElbs;
  }
}