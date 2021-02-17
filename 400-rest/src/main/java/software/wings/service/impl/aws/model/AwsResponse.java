package software.wings.service.impl.aws.model;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public interface AwsResponse extends DelegateTaskNotifyResponseData {
  ExecutionStatus getExecutionStatus();
  String getErrorMessage();
}
