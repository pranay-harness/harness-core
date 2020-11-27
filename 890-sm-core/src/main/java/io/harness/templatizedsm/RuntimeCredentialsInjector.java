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
