package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsAsgListDesiredCapacitiesResponse extends AwsResponse {
  private Map<String, Integer> capacities;

  @Builder
  public AwsAsgListDesiredCapacitiesResponse(
      ExecutionStatus executionStatus, String errorMessage, Map<String, Integer> capacities) {
    super(executionStatus, errorMessage);
    this.capacities = capacities;
  }
}