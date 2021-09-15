/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
