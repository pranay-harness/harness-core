/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
