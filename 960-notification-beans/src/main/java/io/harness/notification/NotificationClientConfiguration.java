package io.harness.notification;

import io.harness.NotificationClientSecrets;
import io.harness.remote.client.ServiceHttpClientConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationClientConfiguration {
  @JsonProperty("messageBroker") NotificationClientBackendConfiguration notificationClientBackendConfiguration;
  @JsonProperty("httpClient") private ServiceHttpClientConfig serviceHttpClientConfig;
  @JsonProperty("secrets") private NotificationClientSecrets notificationSecrets;
}
