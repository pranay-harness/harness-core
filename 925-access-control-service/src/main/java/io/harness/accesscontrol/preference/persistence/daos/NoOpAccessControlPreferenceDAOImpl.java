/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
