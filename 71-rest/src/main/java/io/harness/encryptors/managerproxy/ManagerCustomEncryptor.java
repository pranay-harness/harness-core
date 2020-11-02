package io.harness.encryptors.managerproxy;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.delegatetasks.ValidateSecretReferenceTaskParameters;
import io.harness.encryptors.CustomEncryptor;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomEncryptedDataDetailBuilder;

import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@OwnedBy(PL)
@Singleton
public class ManagerCustomEncryptor implements CustomEncryptor {
  private final ManagerEncryptorHelper managerEncryptorHelper;
  private final CustomEncryptedDataDetailBuilder customEncryptedDataDetailBuilder;

  @Inject
  public ManagerCustomEncryptor(ManagerEncryptorHelper managerEncryptorHelper,
      CustomEncryptedDataDetailBuilder customEncryptedDataDetailBuilder) {
    this.managerEncryptorHelper = managerEncryptorHelper;
    this.customEncryptedDataDetailBuilder = customEncryptedDataDetailBuilder;
  }

  @Override
  public boolean validateReference(
      String accountId, Set<EncryptedDataParams> params, EncryptionConfig encryptionConfig) {
    customEncryptedDataDetailBuilder.buildEncryptedDataDetail(
        EncryptedData.builder().name("Test Secret").parameters(params).build(),
        (CustomSecretsManagerConfig) encryptionConfig);
    ValidateSecretReferenceTaskParameters parameters =
        ValidateSecretReferenceTaskParameters.builder()
            .encryptedRecord(EncryptedRecordData.builder().parameters(params).build())
            .encryptionConfig(encryptionConfig)
            .build();
    return managerEncryptorHelper.validateReference(accountId, parameters);
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    customEncryptedDataDetailBuilder.buildEncryptedDataDetail(
        EncryptedData.builder().name("Test Secret").parameters(encryptedRecord.getParameters()).build(),
        (CustomSecretsManagerConfig) encryptionConfig);
    return managerEncryptorHelper.fetchSecretValue(accountId, encryptedRecord, encryptionConfig);
  }
}
