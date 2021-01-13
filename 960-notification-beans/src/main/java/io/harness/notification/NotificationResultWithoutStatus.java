package io.harness.notification;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationResultWithoutStatus extends NotificationResult {
  @Builder
  public NotificationResultWithoutStatus(String notificationId) {
    super(notificationId);
  }
}
