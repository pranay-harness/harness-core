/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.bases;

import io.harness.ModuleType;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
public class Feature {
  private String name;
  private String description;
  private ModuleType moduleType;
  private Map<Edition, Restriction> restrictions;
}
