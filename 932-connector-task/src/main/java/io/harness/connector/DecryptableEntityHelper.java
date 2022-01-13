/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector;

import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public class DecryptableEntityHelper {
  public DecryptableEntityWithEncryptionConsumers buildDecryptableEntityWithEncryptionConsumers(
      DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptedDataDetails) {
    return DecryptableEntityWithEncryptionConsumers.builder()
        .decryptableEntity(decryptableEntity)
        .encryptedDataDetailList(encryptedDataDetails)
        .build();
  }
}
