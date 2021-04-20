package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.ng.beans.PageResponse;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.SecretTextDTO;

@OwnedBy(PL)
public interface NGSecretService {
  EncryptedData get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  PageResponse<EncryptedData> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm, int page, int size);

  EncryptedData create(SecretTextDTO dto, boolean viaYaml);

  boolean update(SecretTextDTO dto, boolean viaYaml);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
