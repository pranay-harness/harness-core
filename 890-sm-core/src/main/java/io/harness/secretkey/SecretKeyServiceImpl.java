package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;
import io.harness.repositories.SecretKeyRepository;

import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public class SecretKeyServiceImpl implements SecretKeyService {
  private SecretKeyRepository secretKeyRepository;

  @Override
  public SecretKey save(String secretKey) {
    return secretKeyRepository.save(SecretKey.builder().key(secretKey).build());
  }

  @Override
  public Optional<SecretKey> get(String uuid) {
    return secretKeyRepository.findById(uuid);
  }
}
