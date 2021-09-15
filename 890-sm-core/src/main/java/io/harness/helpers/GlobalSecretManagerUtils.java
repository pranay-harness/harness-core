/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.helpers;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class GlobalSecretManagerUtils {
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  public static boolean isNgHarnessSecretManager(NGSecretManagerMetadata ngSecretManagerMetadata) {
    return ngSecretManagerMetadata != null
        && (Boolean.TRUE.equals(ngSecretManagerMetadata.getHarnessManaged())
            || HARNESS_SECRET_MANAGER_IDENTIFIER.equals(ngSecretManagerMetadata.getIdentifier()));
  }
}
