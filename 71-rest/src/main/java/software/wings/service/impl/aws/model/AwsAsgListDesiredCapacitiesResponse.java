package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AwsAsgListDesiredCapacitiesResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private Map<String, Integer> capacities;
}