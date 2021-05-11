package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.entities.NGEncryptedData;
import io.harness.security.encryption.EncryptedDataDetail;

import java.io.InputStream;
import java.util.List;

@OwnedBy(PL)
public interface NGEncryptedDataService {
  NGEncryptedData createSecretText(String accountIdentifier, SecretDTOV2 dto);
  NGEncryptedData createSecretFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream);
  List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity object);
  NGEncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
