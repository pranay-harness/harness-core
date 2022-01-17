/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
