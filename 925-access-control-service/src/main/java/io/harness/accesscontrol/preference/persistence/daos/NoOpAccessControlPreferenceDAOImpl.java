package io.harness.accesscontrol.preference.persistence.daos;

import io.harness.accesscontrol.preference.persistence.models.AccessControlPreference;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public class NoOpAccessControlPreferenceDAOImpl implements AccessControlPreferenceDAO {
  @Override
  public Optional<AccessControlPreference> getByAccountId(String accountId) {
    return Optional.empty();
  }

  @Override
  public AccessControlPreference save(AccessControlPreference accessControlPreference) {
    return accessControlPreference;
  }
}
