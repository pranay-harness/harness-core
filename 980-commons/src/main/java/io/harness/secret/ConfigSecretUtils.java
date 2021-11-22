package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class ConfigSecretUtils {
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
