package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.reflection.ReflectionUtils.getFieldByName;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.SecretManagerType.KMS;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.SecretManagerType;

import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NGEncryptorServiceImpl implements NGEncryptorService {
  private final NGEncryptedDataService encryptedDataService;
  private final KmsEncryptorsRegistry kmsEncryptorsRegistry;
  private final VaultEncryptorsRegistry vaultEncryptorsRegistry;

  @Inject
  public NGEncryptorServiceImpl(NGEncryptedDataService encryptedDataService,
      KmsEncryptorsRegistry kmsEncryptorsRegistry, VaultEncryptorsRegistry vaultEncryptorsRegistry) {
    this.encryptedDataService = encryptedDataService;
    this.kmsEncryptorsRegistry = kmsEncryptorsRegistry;
    this.vaultEncryptorsRegistry = vaultEncryptorsRegistry;
  }

  @Override
  public void decryptEncryptionConfigSecrets(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String projectIdentifier, String orgIdentifier, boolean maskSecrets) {
    if (!maskSecrets) {
      List<DecryptableEntity> decryptableEntities = connectorConfigDTO.getDecryptableEntities();
      for (DecryptableEntity decryptableEntity : decryptableEntities) {
        final BaseNGAccess ngAccess = BaseNGAccess.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .build();
        List<EncryptedDataDetail> encryptedDataDetails =
            encryptedDataService.getEncryptionDetails(ngAccess, decryptableEntity);
        for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
          decryptEncryptionConfigSecrets(accountIdentifier, decryptableEntity, encryptedDataDetail);
        }
      }
    }
  }

  @Override
  public DecryptableEntity decryptEncryptionConfigSecrets2( DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptedDataDetailList, String accountIdentifier) {
    for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetailList) {
      decryptEncryptionConfigSecrets(accountIdentifier, decryptableEntity, encryptedDataDetail);
    }
    return null;
  }


  private void decryptEncryptionConfigSecrets(
      String accountIdentifier, DecryptableEntity decryptableEntity, EncryptedDataDetail encryptedDataDetail) {
    char[] decryptedValue = fetchSecretValue(
        accountIdentifier, encryptedDataDetail.getEncryptedData(), encryptedDataDetail.getEncryptionConfig());
    Field f = getFieldByName(decryptableEntity.getClass(), encryptedDataDetail.getFieldName());

    if (f != null) {
      f.setAccessible(true);
      try {
        SecretRefData secretRefData = (SecretRefData) f.get(decryptableEntity);
        secretRefData.setDecryptedValue(decryptedValue);
      } catch (IllegalAccessException e) {
        throw new InvalidRequestException("Decryption failed for  " + encryptedDataDetail.toString(), e);
      }
    }
  }

  @Override
  public char[] fetchSecretValue(
      String accountIdentifier, EncryptedRecordData encryptedData, EncryptionConfig secretManagerConfig) {
    char[] value;
    SecretManagerType secretManagerType = secretManagerConfig.getType();

    if (secretManagerType == KMS) {
      KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(secretManagerConfig);
      value = kmsEncryptor.fetchSecretValue(accountIdentifier, encryptedData, secretManagerConfig);
    } else if (secretManagerType == VAULT) {
      VaultEncryptor vaultEncryptor =
          vaultEncryptorsRegistry.getVaultEncryptor(secretManagerConfig.getEncryptionType());
      value = vaultEncryptor.fetchSecretValue(accountIdentifier, encryptedData, secretManagerConfig);
    } else {
      throw new UnsupportedOperationException("Secret Manager type not supported: " + secretManagerType);
    }
    if (encryptedData.isBase64Encoded()) {
      byte[] decodedBytes = EncodingUtils.decodeBase64(value);
      value = CHARSET.decode(ByteBuffer.wrap(decodedBytes)).array();
    }
    if (isEmpty(value)) {
      String message = format("Empty or null value returned. Could not migrate secret %s", encryptedData.getName());
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }
    return value;
  }
}
