package io.harness.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MongoBackendConfiguration extends NotificationClientBackendConfiguration {
  @JsonProperty(defaultValue = "mongodb://localhost:27017/notificationChannel")
  @Builder.Default
  @NotEmpty
  private String uri = "mongodb://localhost:27017/notificationChannel";

  @JsonProperty(defaultValue = "30000") @Builder.Default @NotEmpty private int connectTimeout = 30000;

  @JsonProperty(defaultValue = "90000") @Builder.Default @NotEmpty private int serverSelectionTimeout = 90000;

  @JsonProperty(defaultValue = "600000") @Builder.Default @NotEmpty private int maxConnectionIdleTime = 600000;

  @JsonProperty(defaultValue = "300") @Builder.Default @NotEmpty private int connectionsPerHost = 300;
}
