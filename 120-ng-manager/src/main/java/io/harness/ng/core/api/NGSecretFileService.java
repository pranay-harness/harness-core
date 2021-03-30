package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.stream.BoundedInputStream;

@OwnedBy(PL)
public interface NGSecretFileService {
  EncryptedData create(SecretFileDTO dto, BoundedInputStream file);

  boolean update(SecretFileDTO dto, BoundedInputStream file);
}
