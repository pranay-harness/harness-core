package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;
import io.harness.repositories.SecretKeyRepository;
import io.harness.security.encryption.SecretKeyDTO;

import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public abstract class AbstractSecretKeyServiceImpl implements SecretKeyService {
  @Inject private SecretKeyRepository secretKeyRepository;
  private static final byte[] nonce = new byte[32];

  @Override
  public SecretKeyDTO createSecretKey() {
    new SecureRandom().nextBytes(nonce);
    SecretKey secretKey = secretKeyRepository.save(SecretKey.builder().key(nonce).algorithm(getAlgorithm()).build());
    return getSecretKeyDTO(secretKey);
  }

  @Override
  public Optional<SecretKeyDTO> getSecretKey(String uuid) {
    Optional<SecretKey> secretKeyOptional = secretKeyRepository.findById(uuid);
    if (!secretKeyOptional.isPresent()) {
      return Optional.empty();
    }
    SecretKey secretKey = secretKeyOptional.get();
    return Optional.of(getSecretKeyDTO(secretKey));
  }

  private SecretKeyDTO getSecretKeyDTO(SecretKey secretKey) {
    return SecretKeyDTO.builder()
        .uuid(secretKey.getUuid())
        .key(secretKey.getKey())
        .algorithm(secretKey.getAlgorithm())
        .build();
  }
}
