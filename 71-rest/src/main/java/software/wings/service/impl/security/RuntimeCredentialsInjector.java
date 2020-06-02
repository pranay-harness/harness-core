package software.wings.service.impl.security;

import software.wings.beans.SecretManagerConfig;

import java.util.Map;
import java.util.Optional;

public interface RuntimeCredentialsInjector {
  Optional<SecretManagerConfig> updateRuntimeCredentials(
      SecretManagerConfig secretManagerConfig, Map<String, String> runtimeParameters);
}
