package software.wings.service.impl.security;

import static io.harness.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.reflection.ReflectionUtils.getFieldByName;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.KmsOperationException;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.ManagerDecryptionService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 6/7/18.
 */
@Singleton
@Slf4j
public class ManagerDecryptionServiceImpl implements ManagerDecryptionService {
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public void decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isEmpty(encryptedDataDetails)) {
      return;
    }
    // decrypt locally encrypted variables in manager
    encryptedDataDetails.stream()
        .filter(encryptedDataDetail -> encryptedDataDetail.getEncryptionType() == EncryptionType.LOCAL)
        .forEach(encryptedDataDetail -> {
          SimpleEncryption encryption = new SimpleEncryption(encryptedDataDetail.getEncryptedData().getEncryptionKey());
          char[] decryptChars = encryption.decryptChars(encryptedDataDetail.getEncryptedData().getEncryptedValue());
          Field f = getFieldByName(object.getClass(), encryptedDataDetail.getFieldName());
          if (f != null) {
            f.setAccessible(true);
            try {
              f.set(object, decryptChars);
            } catch (IllegalAccessException e) {
              logger.error(format("Decryption failed for %s", encryptedDataDetail.toString()), e);
            }
          }
        });

    // filter non local encrypted values and send to delegate to decrypt
    List<EncryptedDataDetail> nonLocalEncryptedDetails =
        encryptedDataDetails.stream()
            .filter(encryptedDataDetail -> encryptedDataDetail.getEncryptionType() != EncryptionType.LOCAL)
            .collect(Collectors.toList());

    // if nothing left to decrypt return
    if (isEmpty(nonLocalEncryptedDetails)) {
      object.setDecrypted(true);
      return;
    }
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(object.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                          .build();
    try {
      EncryptableSetting decrypted =
          delegateProxyFactory.get(EncryptionService.class, syncTaskContext).decrypt(object, nonLocalEncryptedDetails);
      for (EncryptedDataDetail encryptedDataDetail : nonLocalEncryptedDetails) {
        Field f = getFieldByName(object.getClass(), encryptedDataDetail.getFieldName());
        if (f != null) {
          f.setAccessible(true);
          f.set(object, f.get(decrypted));
        }
      }
      object.setDecrypted(true);
    } catch (Exception e) {
      throw new KmsOperationException(ExceptionUtils.getMessage(e), e, USER);
    }
  }
}
