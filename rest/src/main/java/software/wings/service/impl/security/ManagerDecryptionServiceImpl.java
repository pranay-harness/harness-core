package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

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

import java.util.List;

/**
 * Created by rsingh on 6/7/18.
 */
public class ManagerDecryptionServiceImpl implements ManagerDecryptionService {
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Override
  public Encryptable decrypt(Encryptable object, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isEmpty(encryptedDataDetails)) {
      return object;
    }
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(object.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(EncryptionService.class, syncTaskContext).decrypt(object, encryptedDataDetails);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR).addParam("reason", e.getMessage());
    }
  }
}
