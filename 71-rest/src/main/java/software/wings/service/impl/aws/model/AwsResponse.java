package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

public interface AwsResponse extends DelegateTaskNotifyResponseData {
  ExecutionStatus getExecutionStatus();
  String getErrorMessage();
}