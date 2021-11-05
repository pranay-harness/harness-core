package io.harness.secret;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ConfigSecretUtil {
  public static void resolveSecrets(boolean resolveSecrets, String gcpProject, Object configuration) {
    log.info("Secret resolution started...");
    try {
      if (resolveSecrets) {
        new ConfigSecretResolver(GcpSecretManager.create(gcpProject)).resolveSecret(configuration);
      }
      log.info("Secret resolution finished");
    } catch (Exception e) {
      log.error("Failed to resolve secrets", e);
      throw new RuntimeException(e);
    }
  }
}
