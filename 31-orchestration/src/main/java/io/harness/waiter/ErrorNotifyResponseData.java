package io.harness.waiter;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorNotifyResponseData implements DelegateTaskNotifyResponseData {
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
