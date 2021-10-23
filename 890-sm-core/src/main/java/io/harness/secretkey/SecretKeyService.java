package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public interface SecretKeyService {
  SecretKey save(String secretKey);
  Optional<SecretKey> get(String uuid);
}
