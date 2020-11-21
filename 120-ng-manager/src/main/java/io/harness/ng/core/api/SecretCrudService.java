package io.harness.ng.core.api;

import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.secretmanagerclient.SecretType;

import java.io.InputStream;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.RequestBody;

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

  SecretResponseWrapper create(String accountIdentifier, SecretDTOV2 dto);

  SecretResponseWrapper createViaYaml(String accountIdentifier, SecretDTOV2 dto);

  Optional<SecretResponseWrapper> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  PageResponse<SecretResponseWrapper> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm, int page, int size);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretResponseWrapper createFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream);

  boolean updateFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream);

  boolean update(String accountIdentifier, SecretDTOV2 dto);

  boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto);

  SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretValidationMetaData metadata);
}
