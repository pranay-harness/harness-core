package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsValidateTaskResponse implements AwsDelegateTaskResponse {
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
