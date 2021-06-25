package io.harness.accesscontrol.preference.services;

import io.harness.accesscontrol.preference.daos.AccessControlPreferenceDAO;
import io.harness.accesscontrol.preference.models.AccessControlPreference;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class AccessControlPreferenceServiceImpl implements AccessControlPreferenceService {
  private final AccessControlPreferenceDAO accessControlPreferenceDAO;

  @Override
  public boolean isAccessControlEnabled(String accountIdentifier) {
    return get(accountIdentifier).map(AccessControlPreference::isAccessControlEnabled).orElse(false);
  }

  private Optional<AccessControlPreference> get(String accountIdentifier) {
    return accessControlPreferenceDAO.getByAccountId(accountIdentifier);
  }

  @Override
  public boolean upsertAccessControlEnabled(String accountIdentifier, boolean enabled) {
    Optional<AccessControlPreference> aclPreferenceOptional = get(accountIdentifier);
    AccessControlPreference accessControlPreference =
        aclPreferenceOptional.orElseGet(() -> AccessControlPreference.builder().accountId(accountIdentifier).build());
    accessControlPreference.setAccessControlEnabled(enabled);
    return accessControlPreferenceDAO.save(accessControlPreference) != null;
  }
}
