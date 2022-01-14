package io.harness.accesscontrol.preference.persistence.daos;

import io.harness.accesscontrol.preference.persistence.models.AccessControlPreference;
import io.harness.accesscontrol.preference.persistence.repositories.AccessControlPreferenceRepository;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class AccessControlPreferenceDAOImpl implements AccessControlPreferenceDAO {
  private final AccessControlPreferenceRepository accessControlPreferenceRepository;

  @Override
  public Optional<AccessControlPreference> getByAccountId(String accountId) {
    return accessControlPreferenceRepository.findByAccountId(accountId);
  }

  @Override
  public AccessControlPreference save(AccessControlPreference accessControlPreference) {
    return accessControlPreferenceRepository.save(accessControlPreference);
  }
}
