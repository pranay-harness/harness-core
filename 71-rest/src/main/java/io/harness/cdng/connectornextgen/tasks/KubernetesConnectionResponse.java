package io.harness.cdng.connectornextgen.tasks;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesConnectionResponse implements DelegateTaskNotifyResponseData {
  private Boolean connectionSuccessFul;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;
}
