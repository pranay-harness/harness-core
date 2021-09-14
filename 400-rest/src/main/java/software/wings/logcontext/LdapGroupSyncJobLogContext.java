/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.logcontext;

import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;

public class LdapGroupSyncJobLogContext extends AutoLogContext {
  public LdapGroupSyncJobLogContext(String accountId, String ldapConfigId, OverrideBehavior behavior) {
    super(ImmutableMap.of("accountId", accountId, "ldapConfigId", ldapConfigId), behavior);
  }
}
