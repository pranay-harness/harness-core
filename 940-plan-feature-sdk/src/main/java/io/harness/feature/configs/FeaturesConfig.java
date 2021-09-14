/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.configs;

import io.harness.ModuleType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeaturesConfig {
  ModuleType moduleType;
  List<FeatureInfo> features;
}
