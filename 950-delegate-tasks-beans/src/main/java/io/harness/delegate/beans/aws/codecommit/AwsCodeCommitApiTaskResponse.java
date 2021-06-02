package io.harness.delegate.beans.aws.codecommit;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCodeCommitApiTaskResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private AwsCodeCommitApiResult awsCodecommitApiResult;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
