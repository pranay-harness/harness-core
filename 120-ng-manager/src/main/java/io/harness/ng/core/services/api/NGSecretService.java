package io.harness.ng.core.services.api;

import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import software.wings.security.encryption.EncryptedData;

import java.util.List;

public interface NGSecretService {
  EncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<EncryptedData> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm);

  EncryptedData create(SecretTextCreateDTO dto);

  boolean update(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      SecretTextUpdateDTO dto);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
