/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.stresstesting.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.DEL)
public class DelegateTaskStressTestConfiguration extends Configuration {
  // where the setup location is made
  @JsonProperty("setupLocation") private String setupLocation;
  @JsonProperty("serviceSecret") private String serviceSecret;
  @JsonProperty("target") private String target;
  @JsonProperty("authority") private String authority;
}
