package io.harness.delegate.beans.secrets;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SSHConfigValidationTaskResponse implements DelegateTaskNotifyResponseData {
  private boolean connectionSuccessful;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;
}
