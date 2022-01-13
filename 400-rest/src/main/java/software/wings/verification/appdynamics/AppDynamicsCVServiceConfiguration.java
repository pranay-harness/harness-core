/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.appdynamics;

import software.wings.verification.CVConfiguration;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDynamicsCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Application Name") private String appDynamicsApplicationId;
  @Attributes(required = true, title = "Tier Name") private String tierId;

  @Override
  public CVConfiguration deepCopy() {
    AppDynamicsCVServiceConfiguration clonedConfig = new AppDynamicsCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setAppDynamicsApplicationId(this.getAppDynamicsApplicationId());
    clonedConfig.setTierId(this.getTierId());
    return clonedConfig;
  }
}
