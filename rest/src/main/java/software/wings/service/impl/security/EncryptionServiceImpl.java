package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;
import static software.wings.utils.WingsReflectionUtils.getFieldByName;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by rsingh on 10/18/17.
 */
public class EncryptionServiceImpl implements EncryptionService {
  private static final Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
  @Inject private SecretManagementDelegateService secretManagementDelegateService;

  @Override
  public Encryptable decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails) {
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
      } catch (Exception e) {
        throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, USER, e);
      }
    }
    object.setDecrypted(true);
    return object;
  }

  @Override
  public char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException {
    switch (encryptedDataDetail.getEncryptionType()) {
      case LOCAL:
        SimpleEncryption encryption = new SimpleEncryption(encryptedDataDetail.getEncryptedData().getEncryptionKey());
        return encryption.decryptChars(encryptedDataDetail.getEncryptedData().getEncryptedValue());

      case KMS:
        return secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (KmsConfig) encryptedDataDetail.getEncryptionConfig());

      case VAULT:
        return secretManagementDelegateService.decrypt(
            encryptedDataDetail.getEncryptedData(), (VaultConfig) encryptedDataDetail.getEncryptionConfig());

      default:
        throw new IllegalStateException("invalid encryption type: " + encryptedDataDetail.getEncryptionType());
    }
  }
}
