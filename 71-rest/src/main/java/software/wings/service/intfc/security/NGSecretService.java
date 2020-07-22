package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;
import java.util.Optional;

public interface NGSecretService {
  Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId);

  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);
}
