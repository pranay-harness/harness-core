/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import java.util.Collection;
import java.util.Set;

public interface PremiumFeature extends RestrictedFeature {
  Set<String> getRestrictedAccountTypes();

  boolean isAvailableForAccount(String accountId);

  boolean isAvailable(String accountType);

  boolean isBeingUsed(String accountId);

  Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType);
}
