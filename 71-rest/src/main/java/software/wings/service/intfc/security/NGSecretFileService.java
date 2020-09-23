package software.wings.service.intfc.security;

import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import software.wings.security.encryption.EncryptedData;

import java.io.InputStream;

public interface NGSecretFileService {
  EncryptedData create(SecretFileDTO dto, InputStream file);

  boolean update(
      String account, String org, String project, String identifier, SecretFileUpdateDTO dto, InputStream file);
}
