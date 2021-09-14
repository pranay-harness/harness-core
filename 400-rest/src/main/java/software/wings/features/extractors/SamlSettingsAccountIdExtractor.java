/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.features.extractors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.sso.SamlSettings;
import software.wings.features.api.AccountIdExtractor;

@OwnedBy(PL)
public class SamlSettingsAccountIdExtractor implements AccountIdExtractor<SamlSettings> {
  @Override
  public String getAccountId(SamlSettings samlSettings) {
    return samlSettings.getAccountId();
  }
}
