package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import java.util.List;

@OwnedBy(PL)
public interface NGEncryptorService {
  void decryptEncryptionConfigSecrets(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String projectIdentifier, String orgIdentifier, boolean maskSecrets);

  DecryptableEntity decryptEncryptedDetails(
      DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptedDataDetailList, String accountIdentifier);

  char[] fetchSecretValue(
      String accountIdentifier, EncryptedRecordData encryptedData, EncryptionConfig secretManagerConfig);
}
