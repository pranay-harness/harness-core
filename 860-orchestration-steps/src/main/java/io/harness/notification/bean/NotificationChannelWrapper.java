/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.bean;

import io.harness.notification.channelDetails.PmsNotificationChannel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationChannelWrapper {
  String type;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
  @JsonProperty("spec")
  PmsNotificationChannel notificationChannel;

  @Builder
  public NotificationChannelWrapper(String type, PmsNotificationChannel notificationChannel) {
    this.type = type;
    this.notificationChannel = notificationChannel;
  }
}
