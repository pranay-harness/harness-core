package io.harness.ng.core.api;

import io.harness.beans.EncryptedData;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.stream.BoundedInputStream;

public interface NGSecretFileService {
  EncryptedData create(SecretFileDTO dto, BoundedInputStream file);

  boolean update(SecretFileDTO dto, BoundedInputStream file);
}
