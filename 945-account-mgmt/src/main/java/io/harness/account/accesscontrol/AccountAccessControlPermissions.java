/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.account.accesscontrol;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GTM)
public interface AccountAccessControlPermissions {
  String VIEW_ACCOUNT_PERMISSION = "core_account_view";
  String EDIT_ACCOUNT_PERMISSION = "core_account_edit";
}
