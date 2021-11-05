package io.harness.secret;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ConfigSecretUtil {
  public static void resolveSecrets(SecretsConfiguration secretsConfiguration, Object configuration) {
    try {
      if (!secretsConfiguration.isSecretResolutionEnabled()) {
        log.info("Secret resolution disabled. No secrets will be resolved...");
        return;
      }
      log.info("Secret resolution started...");
      log.info("Fetching secrets from project '{}'", secretsConfiguration.getGcpSecretManagerProject());
      new ConfigSecretResolver(GcpSecretManager.create(secretsConfiguration.getGcpSecretManagerProject()))
          .resolveSecret(configuration);
      log.info("Secret resolution finished");
    } catch (Exception e) {
      log.error("Failed to resolve secrets", e);
      throw new RuntimeException(e);
    }
  }
}
