package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsElbListAppElbsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private List<AwsLoadBalancerDetails> appElbs;
}
