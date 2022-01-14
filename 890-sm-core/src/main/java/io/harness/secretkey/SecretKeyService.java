package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public interface SecretKeyService {
  SecretKey createSecretKey();
  Optional<SecretKey> getSecretKey(String uuid);
  String getAlgorithm();
}
