package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationTaskResponse implements DelegateTaskNotifyResponseData {
  private boolean sent;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
