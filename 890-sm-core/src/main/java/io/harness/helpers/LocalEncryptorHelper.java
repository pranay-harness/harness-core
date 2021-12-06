package io.harness.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.SecretKey;
import io.harness.beans.SecretManagerConfig;
import io.harness.exception.UnexpectedException;
import io.harness.secretkey.SecretKeyConstants;
import io.harness.secretkey.SecretKeyService;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import software.wings.beans.LocalEncryptionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class LocalEncryptorHelper {
  @Inject @Named(SecretKeyConstants.AES_SECRET_KEY) private SecretKeyService secretKeyService;
  @Inject private FeatureFlagHelperService featureFlagService;

  public void populateConfigForEncryption(SecretManagerConfig secretManagerConfig) {
    SecretKey secretKey = secretKeyService.createSecretKey();
    ((LocalEncryptionConfig) secretManagerConfig).setSecretKey(secretKey);
  }

  public void populateConfigForDecryption(EncryptedRecord encryptedRecord, SecretManagerConfig secretManagerConfig) {
    String accountId = secretManagerConfig.getAccountId();
    String secretKeyUuid = null;
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
      secretKeyUuid = encryptedRecord.getEncryptionKey();
    } else if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_MULTI_CRYPTO_MODE)) {
      secretKeyUuid = encryptedRecord.getAdditionalMetadata().getSecretKeyUuid();
    } else {
      return;
    }

    Optional<SecretKey> secretKey = secretKeyService.getSecretKey(secretKeyUuid);
    if (!secretKey.isPresent()) {
      throw new UnexpectedException(String.format("secret key not found for secret key id: %s", secretKeyUuid));
    }

    ((LocalEncryptionConfig) secretManagerConfig).setSecretKey(secretKey.get());
  }
}
