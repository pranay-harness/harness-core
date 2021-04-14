package io.harness.accesscontrol.preference.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public class NoOpAccessControlPreferenceServiceImpl implements AccessControlPreferenceService {
  @Override
  public boolean isAccessControlEnabled(String accountIdentifier) {
    return true;
  }

  @Override
  public boolean upsertAccessControlEnabled(String accountIdentifier, boolean enabled) {
    return true;
  }
}
