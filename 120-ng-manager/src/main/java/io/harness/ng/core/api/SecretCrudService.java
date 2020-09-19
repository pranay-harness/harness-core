package io.harness.ng.core.api;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.secretmanagerclient.SecretType;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.InputStream;
import java.util.Optional;

public interface SecretCrudService {
  default RequestBody getRequestBody(String value) {
    if (!Optional.ofNullable(value).isPresent()) {
      return null;
    }
    return RequestBody.create(MediaType.parse("text/plain"), value);
  }

  default RequestBody getRequestBody(byte[] bytes) {
    return RequestBody.create(MediaType.parse("text/plain"), bytes);
  }

  SecretDTOV2 create(String accountIdentifier, SecretDTOV2 dto);

  SecretDTOV2 createViaYaml(String accountIdentifier, SecretDTOV2 dto);

  Optional<SecretDTOV2> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  NGPageResponse<SecretDTOV2> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm, int page, int size);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretDTOV2 createFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream);

  boolean updateFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream);

  boolean update(String accountIdentifier, SecretDTOV2 dto);

  boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto);

  SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretValidationMetaData metadata);
}
