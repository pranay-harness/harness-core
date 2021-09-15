/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.encryption;

import io.harness.utils.FullyQualifiedIdentifierHelper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretRefDataHelper {
  public String getFullyQualifiedSecretRefString(
      SecretRefData secretRefData, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (secretRefData == null) {
      return null;
    }

    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, secretRefData.getIdentifier());
  }
}
