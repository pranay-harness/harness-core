package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.SecretKeyDTO;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public interface SecretKeyService {
  SecretKeyDTO createSecretKey();
  Optional<SecretKeyDTO> getSecretKey(String uuid);
  String getAlgorithm();
}
