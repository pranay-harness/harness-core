package io.harness.notification.constant;

import io.harness.secret.ConfigSecret;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationClientSecrets {
  @ConfigSecret String notificationClientSecret;
}
