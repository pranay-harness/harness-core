/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MongoBackendConfiguration extends NotificationClientBackendConfiguration {
  private String uri;
  @JsonProperty(defaultValue = "30000") @Builder.Default @NotEmpty private int connectTimeout = 30000;
  @JsonProperty(defaultValue = "90000") @Builder.Default @NotEmpty private int serverSelectionTimeout = 90000;
  @JsonProperty(defaultValue = "600000") @Builder.Default @NotEmpty private int maxConnectionIdleTime = 600000;
  @JsonProperty(defaultValue = "300") @Builder.Default @NotEmpty private int connectionsPerHost = 300;
}
