package io.harness.ng.core.api;

import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.stream.BoundedInputStream;
import software.wings.security.encryption.EncryptedData;

public interface NGSecretFileService {
  EncryptedData create(SecretFileDTO dto, BoundedInputStream file);

  boolean update(SecretFileDTO dto, BoundedInputStream file);
}
