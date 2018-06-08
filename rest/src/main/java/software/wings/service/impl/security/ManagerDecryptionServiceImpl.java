package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.utils.WingsReflectionUtils.getFieldByName;

import com.google.inject.Inject;

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

/**
 * Created by rsingh on 6/7/18.
 */
public class ManagerDecryptionServiceImpl implements ManagerDecryptionService {
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Override
  public void decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isEmpty(encryptedDataDetails)) {
      return;
    }
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(object.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      Encryptable decrypted =
          delegateProxyFactory.get(EncryptionService.class, syncTaskContext).decrypt(object, encryptedDataDetails);
      for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
        Field f = getFieldByName(object.getClass(), encryptedDataDetail.getFieldName());
        f.setAccessible(true);
        f.set(object, f.get(decrypted));
      }
      object.setDecrypted(true);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR).addParam("reason", e.getMessage());
    }
  }
}
