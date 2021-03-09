package io.harness.secrets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretChangeLog;
import io.harness.beans.SecretUpdateData;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.AuditServiceHelper;

import javax.validation.executable.ValidateOnExecution;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static software.wings.beans.Event.Type.CREATE;
import static software.wings.beans.Event.Type.UPDATE;

@ValidateOnExecution
@Singleton
@OwnedBy(PL)
public class SecretsAuditServiceImpl implements SecretsAuditService {
  private final AuditServiceHelper auditServiceHelper;
  private final WingsPersistence wingsPersistence;

  @Inject
  SecretsAuditServiceImpl(AuditServiceHelper auditServiceHelper, WingsPersistence wingsPersistence) {
    this.auditServiceHelper = auditServiceHelper;
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void logSecretCreateEvent(EncryptedData newRecord) {
    auditServiceHelper.reportForAuditingUsingAccountId(newRecord.getAccountId(), null, newRecord, CREATE);
    insertSecretChangeLog(newRecord.getAccountId(), newRecord.getUuid(), "Created");
  }

  @Override
  public void logSecretUpdateEvent(
      EncryptedData oldRecord, EncryptedData updatedRecord, SecretUpdateData secretUpdateData) {
    auditServiceHelper.reportForAuditingUsingAccountId(oldRecord.getAccountId(), oldRecord, updatedRecord, UPDATE);
    insertSecretChangeLog(oldRecord.getAccountId(), oldRecord.getUuid(), secretUpdateData.getChangeSummary());
  }

  @Override
  public void logSecretDeleteEvent(EncryptedData deletedRecord) {
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(deletedRecord.getAccountId(), deletedRecord);
  }

  private void insertSecretChangeLog(String accountId, String encryptedDataId, String auditMessage) {
    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(encryptedDataId)
                                .description(auditMessage)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }
  }
}
