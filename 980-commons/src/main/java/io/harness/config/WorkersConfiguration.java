/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class WorkersConfiguration implements ActiveConfigValidator {
  @JsonProperty("active") Map<String, Boolean> active;
  public boolean confirmWorkerIsActive(Class cls) {
    return isActive(cls, active);
  }
}
