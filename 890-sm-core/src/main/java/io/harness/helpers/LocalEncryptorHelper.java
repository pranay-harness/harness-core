package io.harness.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.SecretManagerConfig;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.exception.UnexpectedException;
import io.harness.secretkey.SecretKeyConstants;
import io.harness.secretkey.SecretKeyService;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.SecretKeyDTO;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import software.wings.beans.LocalEncryptionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class LocalEncryptorHelper {
  @Inject @Named(SecretKeyConstants.AES_SECRET_KEY) private SecretKeyService secretKeyService;
  @Inject private FeatureFlagHelperService featureFlagHelperService;

  public boolean isLocalManagerConfig(SecretManagerConfig secretManagerConfig) {
    return secretManagerConfig instanceof LocalEncryptionConfig;
  }

  public boolean isLocalEncryptor(KmsEncryptor kmsEncryptor) {
    return kmsEncryptor instanceof LocalEncryptor;
  }

  public void populateConfigForEncryption(SecretManagerConfig secretManagerConfig) {
    populateFeatureFlagStatus(secretManagerConfig);
    if (isCofigRequired(secretManagerConfig)) {
      SecretKeyDTO secretKey = secretKeyService.createSecretKey();
      ((LocalEncryptionConfig) secretManagerConfig).setSecretKey(secretKey);
    }
  }

  public void populateConfigForDecryption(EncryptedRecord encryptedRecord, SecretManagerConfig secretManagerConfig) {
    populateFeatureFlagStatus(secretManagerConfig);
    if (isCofigRequired(secretManagerConfig)) {
      String accountId = secretManagerConfig.getAccountId();
      String secretKeyUuid = null;

      if (encryptedRecord.getEncryptedMech() == null) {
        return;
      }

      if (featureFlagHelperService.isEnabled(accountId, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
        secretKeyUuid = encryptedRecord.getEncryptionKey();
      } else if (featureFlagHelperService.isEnabled(accountId, FeatureName.LOCAL_MULTI_CRYPTO_MODE)) {
        secretKeyUuid = encryptedRecord.getAdditionalMetadata().getSecretKeyUuid();
      } else {
        return;
      }

      Optional<SecretKeyDTO> secretKey = secretKeyService.getSecretKey(secretKeyUuid);
      if (!secretKey.isPresent()) {
        throw new UnexpectedException(String.format("secret key not found for secret key id: %s", secretKeyUuid));
      }

      ((LocalEncryptionConfig) secretManagerConfig).setSecretKey(secretKey.get());
    }
  }

  private void populateFeatureFlagStatus(SecretManagerConfig secretManagerConfig) {
    Map<String, Boolean> flagStatus = secretManagerConfig.getEncryptionFeatureFlagStatus();
    if (featureFlagHelperService.isEnabled(
            secretManagerConfig.getAccountId(), FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
      flagStatus.put(FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE.name(), Boolean.TRUE);
    }
    if (featureFlagHelperService.isEnabled(secretManagerConfig.getAccountId(), FeatureName.LOCAL_MULTI_CRYPTO_MODE)) {
      flagStatus.put(FeatureName.LOCAL_MULTI_CRYPTO_MODE.name(), Boolean.TRUE);
    }
    ((LocalEncryptionConfig) secretManagerConfig).setEncryptionFeatureFlagStatus(flagStatus);
  }

  private boolean isCofigRequired(SecretManagerConfig secretManagerConfig) {
    Map<String, Boolean> flagStatus = secretManagerConfig.getEncryptionFeatureFlagStatus();
    return Boolean.TRUE.equals(flagStatus.get(FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE.name()))
        || Boolean.TRUE.equals(flagStatus.get(FeatureName.LOCAL_MULTI_CRYPTO_MODE.name()));
  }
}
