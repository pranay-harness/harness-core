package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;
import io.harness.repositories.SecretKeyRepository;

import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;

@OwnedBy(HarnessTeam.PL)
public abstract class AbstractSecretKeyServiceImpl implements SecretKeyService {
  @Inject private SecretKeyRepository secretKeyRepository;

  @Override
  public SecretKey createSecretKey() {
    byte[] nonce = new byte[32];
    new SecureRandom().nextBytes(nonce);
    return secretKeyRepository.save(
        SecretKey.builder().secretKeySpec(new SecretKeySpec(nonce, getAlgorithm())).build());
  }

  @Override
  public Optional<SecretKey> getSecretKey(String uuid) {
    return secretKeyRepository.findById(uuid);
  }

  protected abstract String getAlgorithm();
}
