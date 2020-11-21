package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsElbListTargetGroupsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private Map<String, String> targetGroups;
}
