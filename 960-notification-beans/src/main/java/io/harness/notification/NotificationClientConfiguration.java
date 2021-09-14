/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification;

import io.harness.notification.constant.NotificationClientSecrets;
import io.harness.remote.client.ServiceHttpClientConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationClientConfiguration {
  @JsonProperty("messageBroker") NotificationClientBackendConfiguration notificationClientBackendConfiguration;
  @JsonProperty("httpClient") private ServiceHttpClientConfig serviceHttpClientConfig;
  @JsonProperty("secrets") private NotificationClientSecrets notificationSecrets;
}
