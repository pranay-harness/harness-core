/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
