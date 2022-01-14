/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.security.encryption;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class AdditionalMetadata {
  public static final String SECRET_KEY_UUID_KEY = "SecretKey";
  public static final String AWS_ENCRYPTED_SECRET = "AwsEncryptedSecret";

  @Singular private Map<String, Object> values;

  public Map<String, Object> addValues(Map<String, Object> newValues) {
    newValues.forEach(values::put);
    return values;
  }

  public String getSecretKeyUuid() {
    return (String) values.get(SECRET_KEY_UUID_KEY);
  }

  public byte[] getAwsEncryptedSecret() {
    return (byte[]) values.get(AWS_ENCRYPTED_SECRET);
  }
}
