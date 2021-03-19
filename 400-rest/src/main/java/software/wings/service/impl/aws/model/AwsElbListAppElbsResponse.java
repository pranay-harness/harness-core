package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsElbListAppElbsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private List<AwsLoadBalancerDetails> appElbs;
}
