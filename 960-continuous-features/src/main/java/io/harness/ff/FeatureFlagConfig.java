/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
