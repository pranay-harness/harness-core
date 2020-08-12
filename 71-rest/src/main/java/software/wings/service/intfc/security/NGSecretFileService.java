package software.wings.service.intfc.security;

import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.stream.BoundedInputStream;
import software.wings.security.encryption.EncryptedData;

public interface NGSecretFileService {
  EncryptedData create(SecretFileDTO dto, BoundedInputStream file);

  boolean update(
      String account, String org, String project, String identifier, SecretFileUpdateDTO dto, BoundedInputStream file);
}
