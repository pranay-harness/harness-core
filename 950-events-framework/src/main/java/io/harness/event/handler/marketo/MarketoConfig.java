/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handler.marketo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rktummala
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class MarketoConfig {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String url;
  private String clientId;
  private String clientSecret;
}
