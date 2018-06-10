package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.utils.WingsReflectionUtils.getFieldByName;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.ManagerDecryptionService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 6/7/18.
 */
public class ManagerDecryptionServiceImpl implements ManagerDecryptionService {
  private static final Logger logger = LoggerFactory.getLogger(ManagerDecryptionServiceImpl.class);

  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public void decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isEmpty(encryptedDataDetails)) {
      return;
    }
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(object.getAccountId())
                                          .withAppId(Base.GLOBAL_APP_ID)
                                          .withTimeout(TimeUnit.SECONDS.toMillis(10L))
                                          .build();
    try {
      Encryptable decrypted = timeLimiter.callWithTimeout(() -> {
        while (true) {
          try {
            return delegateProxyFactory.get(EncryptionService.class, syncTaskContext)
                .decrypt(object, encryptedDataDetails);
          } catch (Exception e) {
            logger.warn("Error decrypting value. Retrying.");
          }
        }
      }, 65, TimeUnit.SECONDS, true);
      for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
        Field f = getFieldByName(object.getClass(), encryptedDataDetail.getFieldName());
        if (f != null) {
          f.setAccessible(true);
          f.set(object, f.get(decrypted));
        }
      }
      object.setDecrypted(true);
    } catch (UncheckedTimeoutException ex) {
      logger.warn("Timed out decrypting value", ex);
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR).addParam("reason", "Timed out decrypting value");
    } catch (Exception e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, e).addParam("reason", e.getMessage());
    }
  }
}
