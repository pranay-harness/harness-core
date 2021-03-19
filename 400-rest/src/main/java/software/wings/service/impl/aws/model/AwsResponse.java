package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public interface AwsResponse extends DelegateTaskNotifyResponseData {
  ExecutionStatus getExecutionStatus();
  String getErrorMessage();
}
