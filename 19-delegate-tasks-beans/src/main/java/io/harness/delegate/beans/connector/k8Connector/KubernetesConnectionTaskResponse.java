package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesConnectionTaskResponse implements DelegateTaskNotifyResponseData {
  private Boolean connectionSuccessFul;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;
  private Exception exception;
}
