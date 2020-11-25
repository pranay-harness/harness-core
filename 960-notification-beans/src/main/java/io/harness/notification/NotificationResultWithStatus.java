package io.harness.notification;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
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
