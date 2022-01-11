/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.client.services;

import io.harness.enforcement.client.CustomRestrictionRegisterConfiguration;
import io.harness.enforcement.client.RestrictionUsageRegisterConfiguration;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.enforcement.constants.FeatureRestrictionName;

public interface EnforcementSdkRegisterService {
  void initialize(RestrictionUsageRegisterConfiguration restrictionUsageRegisterConfiguration,
      CustomRestrictionRegisterConfiguration customRestrictionRegisterConfiguration);
  RestrictionUsageInterface getRestrictionUsageInterface(FeatureRestrictionName featureRestrictionName);
  CustomRestrictionInterface getCustomRestrictionInterface(FeatureRestrictionName featureRestrictionName);
}
