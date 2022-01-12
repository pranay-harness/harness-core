/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.helper;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DecryptableEntityHelper;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.remote.client.NGRestClientExecutor;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import java.util.List;

public class DecryptionHelperViaManager implements DecryptionHelper {
  @Inject SecretNGManagerClient ngSecretDecryptionClient;
  @Inject NGRestClientExecutor restClientExecutor;
  @Inject DecryptableEntityHelper decryptableEntityHelper;
  @Override
  public DecryptableEntity decrypt(DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptionDetails) {
    final DecryptableEntityWithEncryptionConsumers build =
        decryptableEntityHelper.buildDecryptableEntityWithEncryptionConsumers(decryptableEntity, encryptionDetails);
    return restClientExecutor.getResponse(ngSecretDecryptionClient.decryptEncryptedDetails(build, "random"));
  }
}
