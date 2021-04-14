package io.harness.accesscontrol.preference.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlPreferenceService {
  boolean isAccessControlEnabled(String accountIdentifier);

  boolean upsertAccessControlEnabled(String accountIdentifier, boolean enabled);
}
