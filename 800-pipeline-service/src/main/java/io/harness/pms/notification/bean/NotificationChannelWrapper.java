package io.harness.pms.notification.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.pms.notification.channelDetails.PmsNotificationChannel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationChannelWrapper {
  String type;

  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  PmsNotificationChannel notificationChannel;

  @Builder
  public NotificationChannelWrapper(String type, PmsNotificationChannel notificationChannel) {
    this.type = type;
    this.notificationChannel = notificationChannel;
  }
}
