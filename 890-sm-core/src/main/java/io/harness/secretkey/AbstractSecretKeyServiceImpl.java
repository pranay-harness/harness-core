package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;
import io.harness.repositories.SecretKeyRepository;

import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public abstract class AbstractSecretKeyServiceImpl implements SecretKeyService {
  @Inject private SecretKeyRepository secretKeyRepository;
  private static final byte[] nonce = new byte[32];

  @Override
  public SecretKey createSecretKey() {
    new SecureRandom().nextBytes(nonce);
    return secretKeyRepository.save(SecretKey.builder().key(nonce).algorithm(getAlgorithm()).build());
  }

  @Override
  public Optional<SecretKey> getSecretKey(String uuid) {
    return secretKeyRepository.findById(uuid);
  }
}
