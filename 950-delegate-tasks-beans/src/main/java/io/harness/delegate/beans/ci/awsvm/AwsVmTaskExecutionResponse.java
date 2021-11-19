package io.harness.delegate.beans.ci.awsvm;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsVmTaskExecutionResponse implements CITaskExecutionResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
  @Builder.Default private static final CITaskExecutionResponse.Type type = Type.AWS_VM;

  @Override
  public CITaskExecutionResponse.Type getType() {
    return type;
  }
}