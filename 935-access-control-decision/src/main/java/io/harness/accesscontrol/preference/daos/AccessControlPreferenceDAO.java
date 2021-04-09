package io.harness.accesscontrol.preference.daos;

import io.harness.accesscontrol.preference.models.AccessControlPreference;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlPreferenceDAO {
  Optional<AccessControlPreference> getByAccountId(String accountId);

  AccessControlPreference save(AccessControlPreference accessControlPreference);
}
