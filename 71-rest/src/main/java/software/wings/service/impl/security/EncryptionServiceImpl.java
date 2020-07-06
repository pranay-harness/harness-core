package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.reflection.ReflectionUtils.getFieldByName;
import static io.harness.security.SimpleEncryption.CHARSET;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.exception.ExceptionUtils;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by rsingh on 10/18/17.
 */
@Singleton
@Slf4j
public class EncryptionServiceImpl implements EncryptionService {
  private SecretManagementDelegateService secretManagementDelegateService;
  private ExecutorService threadPoolExecutor;

  @Inject
  public EncryptionServiceImpl(SecretManagementDelegateService secretManagementDelegateService,
      @Named("asyncExecutor") ExecutorService threadPoolExecutor) {
    this.secretManagementDelegateService = secretManagementDelegateService;
    this.threadPoolExecutor = threadPoolExecutor;
  }

  @Override
  public EncryptableSetting decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails) {
    logger.debug("Decrypting a secret");
    if (isEmpty(encryptedDataDetails)) {
      return object;
    }

    for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
      try {
        char[] decryptedValue;

        Field f = getFieldByName(object.getClass(), encryptedDataDetail.getFieldName());
        if (f == null) {
          logger.warn("Could not find field {} in class {}", encryptedDataDetail.getFieldName(), object.getClass());
          continue;
        }
        Preconditions.checkNotNull(f, "could not find " + encryptedDataDetail.getFieldName() + " in " + object);
        f.setAccessible(true);

        decryptedValue = getDecryptedValue(encryptedDataDetail);
        f.set(object, decryptedValue);
        Field encryptedRefField = getEncryptedRefField(f, object);
        encryptedRefField.setAccessible(true);
        encryptedRefField.set(object, null);
      } catch (DelegateRetryableException | SecretManagementDelegateException e) {
        throw e;
      } catch (Exception e) {
        // Log the root cause exception of failed decryption attempts.
        logger.error("Failed to decrypt encrypted settings.", e);
        throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, ExceptionUtils.getMessage(e), USER);
      }
    }
    object.setDecrypted(true);
    return object;
  }

  @Override
  public List<EncryptableSettingWithEncryptionDetails> decrypt(
      List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList) {
    List<Future<EncryptableSetting>> futures = new ArrayList<>();
    for (EncryptableSettingWithEncryptionDetails encryptableSettingWithEncryptionDetails :
        encryptableSettingWithEncryptionDetailsList) {
      EncryptableSetting encryptableSetting = encryptableSettingWithEncryptionDetails.getEncryptableSetting();
      futures.add(threadPoolExecutor.submit(
          () -> decrypt(encryptableSetting, encryptableSettingWithEncryptionDetails.getEncryptedDataDetails())));
    }

    for (int i = 0; i < encryptableSettingWithEncryptionDetailsList.size(); i++) {
      try {
        EncryptableSetting encryptableSetting = futures.get(i).get();
        encryptableSettingWithEncryptionDetailsList.get(i).setEncryptableSetting(encryptableSetting);
      } catch (ExecutionException e) {
        logger.error("Failed to batch process decryption request of encrypted settings.", e.getCause());
        throw new SecretManagementException(
            ENCRYPT_DECRYPT_ERROR, "Failed to batch process decryption request of encrypted settings", USER);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Thread was interrupted during execution", e);
        throw new SecretManagementException(
            ENCRYPT_DECRYPT_ERROR, "Failed to batch process decryption request of encrypted settings", USER);
      }
    }

    return encryptableSettingWithEncryptionDetailsList;
  }

  @Override
  public char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) {
    char[] decryptedValue;
    switch (encryptedDataDetail.getEncryptionConfig().getEncryptionType()) {
      case LOCAL:
        SimpleEncryption encryption = new SimpleEncryption(encryptedDataDetail.getEncryptedData().getEncryptionKey());
        decryptedValue = encryption.decryptChars(encryptedDataDetail.getEncryptedData().getEncryptedValue());
        break;

      case KMS:
        decryptedValue = secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (KmsConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      case GCP_KMS:
        decryptedValue = secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (GcpKmsConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      case VAULT:
        decryptedValue = secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (VaultConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      case AWS_SECRETS_MANAGER:
        decryptedValue = secretManagementDelegateService.decrypt(encryptedDataDetail.getEncryptedData(),
            (AwsSecretsManagerConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      case AZURE_VAULT:
        decryptedValue = secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (AzureVaultConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      case CYBERARK:
        decryptedValue = secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (CyberArkConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      case CUSTOM:
        decryptedValue = secretManagementDelegateService.decrypt(encryptedDataDetail.getEncryptedData(),
            (CustomSecretsManagerConfig) encryptedDataDetail.getEncryptionConfig());
        break;

      default:
        throw new IllegalStateException(
            "invalid encryption type: " + encryptedDataDetail.getEncryptedData().getEncryptionType());
    }

    if (decryptedValue != null && encryptedDataDetail.getEncryptedData().isBase64Encoded()) {
      byte[] decodedBytes = EncodingUtils.decodeBase64(decryptedValue);
      decryptedValue = CHARSET.decode(ByteBuffer.wrap(decodedBytes)).array();
    }

    return decryptedValue;
  }
}
