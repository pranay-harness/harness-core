package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.reflection.ReflectionUtils.getFieldByName;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

@OwnedBy(PL)
@Slf4j
public class SecretDecryptionServiceImpl implements SecretDecryptionService {
  @Inject private EncryptionService encryptionService;

  @Override
  public DecryptableEntity decrypt(DecryptableEntity object, List<EncryptedDataDetail> encryptedDataDetails) {
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
        f.setAccessible(true);

        decryptedValue = getDecryptedValue(encryptedDataDetail);
        SecretRefData secretRefData = (SecretRefData) f.get(object);
        secretRefData.setDecryptedValue(decryptedValue);
        f.set(object, secretRefData);
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
  public char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException {
    return encryptionService.getDecryptedValue(encryptedDataDetail);
  }
}
