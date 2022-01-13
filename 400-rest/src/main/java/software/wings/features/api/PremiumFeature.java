/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
