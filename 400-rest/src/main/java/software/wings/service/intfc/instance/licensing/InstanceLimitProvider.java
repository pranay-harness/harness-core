/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.instance.licensing;

import software.wings.beans.AccountType;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public interface InstanceLimitProvider {
  Map<String, Integer> DEFAULT_SI_USAGE_LIMITS = ImmutableMap.of(
      AccountType.COMMUNITY, 50, AccountType.PAID, 1500, AccountType.TRIAL, 50, AccountType.ESSENTIALS, 1500);

  static Integer defaults(String accountType) {
    if (!AccountType.isValid(accountType)) {
      throw new IllegalArgumentException("Invalid account type: " + accountType);
    }
    return DEFAULT_SI_USAGE_LIMITS.get(accountType.toUpperCase());
  }

  /**
   * Gets the maximum number of instances allowed for a given account based on their license.
   *
   * @param accountId account Id
   * @return max number of allowed instances
   */
  long getAllowedInstances(String accountId);
}
