package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;
import io.harness.repositories.SecretKeyRepository;

import java.security.SecureRandom;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;

@OwnedBy(HarnessTeam.PL)
public class AESSecretKeyServiceImpl implements SecretKeyService {
  private SecretKeyRepository secretKeyRepository;

  @Override
  public SecretKey createSecretKey() {
    byte[] nonce = new byte[32];
    new SecureRandom().nextBytes(nonce);
    return secretKeyRepository.save(SecretKey.builder().secretKeySpec(new SecretKeySpec(nonce, "AES")).build());
  }

  @Override
  public Optional<SecretKey> getSecretKey(String uuid) {
    return secretKeyRepository.findById(uuid);
  }
}
