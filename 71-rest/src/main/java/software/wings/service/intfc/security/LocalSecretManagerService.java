package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.LocalEncryptionConfig;

/**
 * @author marklu on 2019-05-14
 */
@OwnedBy(PL)
public interface LocalSecretManagerService {
  LocalEncryptionConfig getEncryptionConfig(String accountId);

  String saveLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig);

  void validateLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig);
}
