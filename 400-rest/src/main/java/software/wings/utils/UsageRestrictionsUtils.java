/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
