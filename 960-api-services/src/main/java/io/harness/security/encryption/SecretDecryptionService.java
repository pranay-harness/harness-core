/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.security.encryption;

import io.harness.beans.DecryptableEntity;

import software.wings.annotation.EncryptableSetting;

import java.io.IOException;
import java.util.List;

public interface SecretDecryptionService {
  DecryptableEntity decrypt(DecryptableEntity object, List<EncryptedDataDetail> encryptedDataDetails);

  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException;

  EncryptableSetting decrypt(
      EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails, boolean fromCache);
}
