/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.helper.DecryptionHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;

public class DecryptionHelperDelegate implements DecryptionHelper {
  @Inject private SecretDecryptionService secretDecryptionService;

  @Override
  public DecryptableEntity decrypt(DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptionDetails) {
    return secretDecryptionService.decrypt(decryptableEntity, encryptionDetails);
  }
}
