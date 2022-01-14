package io.harness.delegate.beans.ci.k8s;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sTaskExecutionResponse implements CITaskExecutionResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private CiK8sTaskResponse k8sTaskResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
  @Builder.Default private static final CITaskExecutionResponse.Type type = Type.K8;

  @Override
  public CITaskExecutionResponse.Type getType() {
    return type;
  }
}
