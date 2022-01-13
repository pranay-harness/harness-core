/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
