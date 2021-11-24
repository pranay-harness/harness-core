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
    return restClientExecutor.getResponse(ngSecretDecryptionClient.decryptEncryptedDetails(build, null));
  }
}
