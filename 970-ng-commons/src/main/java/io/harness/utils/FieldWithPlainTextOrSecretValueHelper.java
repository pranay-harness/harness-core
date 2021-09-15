/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidArgumentsException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FieldWithPlainTextOrSecretValueHelper {
  public char[] getValueFromPlainTextOrSecretRef(String plainText, SecretRefData secretRefData) {
    if (isNotBlank(plainText) && !isNullSecretRefData(secretRefData)) {
      throw new InvalidArgumentsException("Both plain text and secret value specified for the field");
    }
    if (isBlank(plainText) && isNullSecretRefData(secretRefData)) {
      throw new InvalidArgumentsException("Both plain text and secret value cannot be null for the field");
    }
    if (isNotBlank(plainText)) {
      return plainText.toCharArray();
    }
    return secretRefData.getDecryptedValue();
  }

  private static boolean isNullSecretRefData(SecretRefData secretRefData) {
    return secretRefData == null || secretRefData.isNull();
  }

  public String getSecretAsStringFromPlainTextOrSecretRef(String plainText, SecretRefData secretRefData) {
    char[] secretValue = getValueFromPlainTextOrSecretRef(plainText, secretRefData);
    return EmptyPredicate.isNotEmpty(secretValue) ? String.valueOf(secretValue) : null;
  }
}
