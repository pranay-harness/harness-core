package io.harness.delegate.beans;

import io.harness.notification.beans.NotificationProcessingResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationTaskResponse implements DelegateTaskNotifyResponseData {
  private NotificationProcessingResponse processingResponse;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
