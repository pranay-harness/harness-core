/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.UsageRestrictions;
import software.wings.security.UsageRestrictions.AppEnvRestriction;

import com.google.common.collect.Sets;

/**
 * Helper class to
 * @author rktummala on 10/18/2018
 */
public class UsageRestrictionsUtils {
  public static UsageRestrictions getAllAppAllEnvUsageRestrictions() {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    EnvFilter prodEnvFilter = EnvFilter.builder().filterTypes(Sets.newHashSet(EnvFilter.FilterType.PROD)).build();
    EnvFilter nonprodEnvFilter =
        EnvFilter.builder().filterTypes(Sets.newHashSet(EnvFilter.FilterType.NON_PROD)).build();
    AppEnvRestriction prodAppEnvRestriction =
        AppEnvRestriction.builder().appFilter(appFilter).envFilter(prodEnvFilter).build();
    AppEnvRestriction nonProdAppEnvRestriction =
        AppEnvRestriction.builder().appFilter(appFilter).envFilter(nonprodEnvFilter).build();
    return UsageRestrictions.builder()
        .appEnvRestrictions(Sets.newHashSet(prodAppEnvRestriction, nonProdAppEnvRestriction))
        .build();
  }
}
