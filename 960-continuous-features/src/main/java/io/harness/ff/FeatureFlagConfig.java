/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ff;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CF)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class FeatureFlagConfig {
  /**
   * This will be the default system for easy management for developers locally and for ON-PREM
   */
  @Default private FeatureFlagSystem featureFlagSystem = FeatureFlagSystem.LOCAL;
  /**
   * If this feature is turned on, it will sync featureFlags from FeatureName to CF -> Create featureFlags if not
   * present
   */
  private boolean syncFeaturesToCF;
}
