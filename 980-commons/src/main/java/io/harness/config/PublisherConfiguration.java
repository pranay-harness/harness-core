/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PublisherConfiguration implements ActiveConfigValidator {
  @JsonProperty("active") Map<String, Boolean> active;
  public boolean isPublisherActive(Class cls) {
    return isActive(cls, active);
  }

  public static PublisherConfiguration allOn() {
    return new PublisherConfiguration();
  }

  public static PublisherConfiguration allOff() {
    final PublisherConfiguration publisherConfiguration = new PublisherConfiguration();
    publisherConfiguration.setActive(ImmutableMap.<String, Boolean>builder()
                                         .put("io.harness", Boolean.FALSE)
                                         .put("software.wings", Boolean.FALSE)
                                         .build());
    return publisherConfiguration;
  }
}
