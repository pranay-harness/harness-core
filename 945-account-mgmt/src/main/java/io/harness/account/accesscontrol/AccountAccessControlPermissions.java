package io.harness.account.accesscontrol;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GTM)
public interface AccountAccessControlPermissions {
  String VIEW_ACCOUNT_PERMISSION = "core_account_view";
  String EDIT_ACCOUNT_PERMISSION = "core_account_edit";
}
