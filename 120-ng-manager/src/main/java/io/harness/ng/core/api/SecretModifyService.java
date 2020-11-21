package io.harness.ng.core.api;

import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;

import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public interface SecretModifyService {
  default RequestBody getRequestBody(String value) {
    if (!Optional.ofNullable(value).isPresent()) {
      return null;
    }
    return RequestBody.create(MediaType.parse("text/plain"), value);
  }

  default RequestBody getRequestBody(byte[] bytes) {
    return RequestBody.create(MediaType.parse("text/plain"), bytes);
  }

  EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto);

  boolean update(String accountIdentifier, SecretDTOV2 dto);

  boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto);
}
