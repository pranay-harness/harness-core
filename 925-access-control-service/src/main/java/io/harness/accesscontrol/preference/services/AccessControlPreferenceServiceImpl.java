/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.preference.services;

import io.harness.accesscontrol.preference.persistence.daos.AccessControlPreferenceDAO;
import io.harness.accesscontrol.preference.persistence.models.AccessControlPreference;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessControlPreferenceServiceImpl implements AccessControlPreferenceService {
  private final AccessControlPreferenceDAO accessControlPreferenceDAO;

  @Inject
  public AccessControlPreferenceServiceImpl(AccessControlPreferenceDAO accessControlPreferenceDAO) {
    this.accessControlPreferenceDAO = accessControlPreferenceDAO;
  }

  @Override
  public boolean isAccessControlEnabled(String accountIdentifier) {
    Optional<AccessControlPreference> accessControlPreferenceOptional = get(accountIdentifier);
    return accessControlPreferenceOptional.map(AccessControlPreference::isAccessControlEnabled).orElse(true);
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
