package io.harness.ng.core.api;

import io.harness.beans.NGPageResponse;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import software.wings.security.encryption.EncryptedData;

public interface NGSecretService {
  EncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  NGPageResponse<EncryptedData> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm, int page, int size);

  EncryptedData create(SecretTextDTO dto, boolean viaYaml);

  boolean update(SecretTextDTO dto, boolean viaYaml);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
