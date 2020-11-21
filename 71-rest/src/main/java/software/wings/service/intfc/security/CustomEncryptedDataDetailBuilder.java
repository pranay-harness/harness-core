package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

@OwnedBy(PL)
public interface CustomEncryptedDataDetailBuilder {
  EncryptedDataDetail buildEncryptedDataDetail(
      EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
}
