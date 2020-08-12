package io.harness.ng.core.api;

import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import software.wings.security.encryption.EncryptedData;

import java.util.List;

public interface NGSecretService {
  EncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<EncryptedData> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm);

  EncryptedData create(SecretTextDTO dto, boolean viaYaml);

  boolean update(SecretTextDTO dto, boolean viaYaml);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
