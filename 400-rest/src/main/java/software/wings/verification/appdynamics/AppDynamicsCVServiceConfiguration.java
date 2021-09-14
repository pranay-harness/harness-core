/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
