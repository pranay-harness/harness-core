package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

public interface AwsDelegateTaskResponse extends DelegateTaskNotifyResponseData {
  CommandExecutionStatus getExecutionStatus();

  String getErrorMessage();
}
