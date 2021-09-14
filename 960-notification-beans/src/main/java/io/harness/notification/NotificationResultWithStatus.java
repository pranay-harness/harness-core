/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationResultWithStatus extends NotificationResult {
  private NotificationStatus status;
  private String errorMessage;

  @Builder
  public NotificationResultWithStatus(NotificationStatus status, String errorMessage, String notificationId) {
    super(notificationId);
    this.status = status;
    this.errorMessage = errorMessage;
  }
}
