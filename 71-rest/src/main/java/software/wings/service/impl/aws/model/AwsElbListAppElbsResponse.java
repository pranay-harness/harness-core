package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AwsElbListAppElbsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private List<AwsLoadBalancerDetails> appElbs;
}
