/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
      try (SecretStorage secretStorage = GcpSecretManager.create(secretsConfiguration.getGcpSecretManagerProject())) {
        new ConfigSecretResolver(secretStorage).resolveSecret(configuration);
      }
      log.info("Secret resolution finished");
    } catch (Exception e) {
      log.error("Failed to resolve secrets", e);
      throw new RuntimeException(e);
    }
  }
}
