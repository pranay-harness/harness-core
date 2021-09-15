/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.features.extractors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.sso.LdapSettings;
import software.wings.features.api.AccountIdExtractor;

@OwnedBy(PL)
public class LdapSettingsAccountIdExtractor implements AccountIdExtractor<LdapSettings> {
  @Override
  public String getAccountId(LdapSettings ldapSettings) {
    return ldapSettings.getAccountId();
  }
}
