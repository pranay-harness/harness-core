package software.wings.service.impl.security;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.delegatetasks.CommandTask;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsDelegateService;
import software.wings.utils.WingsReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/18/17.
 */
public class EncryptionServiceImpl implements EncryptionService {
  private static final Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
  @Inject private KmsDelegateService kmsDelegateService;

  @Override
  public void decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails) {
    if (encryptedDataDetails == null || encryptedDataDetails.isEmpty()) {
      return;
    }

    for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
      try {
        char[] decryptedValue = null;

        Field f = WingsReflectionUtils.getFieldByName(object.getClass(), encryptedDataDetail.getFieldName());
        if (f == null) {
          logger.warn(
              "Could not find field {} " + encryptedDataDetail.getFieldName() + " in class " + object.getClass());
          continue;
        }
        Preconditions.checkNotNull(f, "could not find " + encryptedDataDetail.getFieldName() + " in " + object);
        f.setAccessible(true);

        switch (encryptedDataDetail.getEncryptionType()) {
          case LOCAL:
            SimpleEncryption encryption = new SimpleEncryption(object.getAccountId());
            decryptedValue = encryption.decryptChars(encryptedDataDetail.getEncryptedData().getEncryptedValue());
            break;

          case KMS:
            decryptedValue =
                kmsDelegateService.decrypt(encryptedDataDetail.getEncryptedData(), encryptedDataDetail.getKmsConfig());
            break;

          default:
            throw new IllegalStateException("invalid encryption type: " + encryptedDataDetail.getEncryptionType());
        }
        f.set(object, decryptedValue);
      } catch (Exception e) {
        throw new WingsException(e);
      }
    }
    object.setDecrypted(true);
  }
}
