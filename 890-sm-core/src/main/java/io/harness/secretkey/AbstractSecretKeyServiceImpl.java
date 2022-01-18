/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
