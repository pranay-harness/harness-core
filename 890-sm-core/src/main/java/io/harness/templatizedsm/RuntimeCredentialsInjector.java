/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.templatizedsm;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;

import java.util.Map;
import java.util.Optional;

@OwnedBy(PL)
public interface RuntimeCredentialsInjector {
  Optional<SecretManagerConfig> updateRuntimeCredentials(
      SecretManagerConfig secretManagerConfig, Map<String, String> runtimeParameters, boolean shouldUpdateVaultConfig);
}
